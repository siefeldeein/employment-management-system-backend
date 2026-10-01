/* Renders every docs/*.md file into a styled A4 PDF in docs/pdf/.
 *
 *   npm install
 *   npm run build
 *
 * Requires a local Chrome/Edge installation; puppeteer-core does not download
 * its own browser. Set CHROME_PATH if yours is installed somewhere unusual.
 *
 * Pipeline per document:
 *   1. marked parses the Markdown to HTML.
 *   2. highlight.js colours the fenced code blocks in Node, so no browser
 *      bundle has to be built or inlined.
 *   3. _pdf-toc.js runs in the page: it gives every heading a stable id,
 *      normalises tables and callouts, and builds the contents page.
 *   4. Chrome prints to PDF. The contents page numbers are predicted from the
 *      layout, then measured back out of the printed PDF and corrected, so the
 *      numbers printed in the contents are the numbers the reader will see.
 */
import { marked } from "marked";
import hljs from "highlight.js";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import puppeteer from "puppeteer-core";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const DOCS = path.resolve(HERE, "..");
const OUT = path.join(DOCS, "pdf");
const WORK = path.join(HERE, "work");

const CHROME =
  process.env.CHROME_PATH ||
  [
    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
    "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
    "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
    "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/usr/bin/google-chrome",
    "/usr/bin/chromium",
  ].find((p) => fs.existsSync(p));

if (!CHROME) {
  throw new Error("No Chrome or Edge found. Set CHROME_PATH to the executable.");
}

/* ---------- print geometry - must match page.pdf() at the bottom ---------- */
const MARGIN = { top: "20mm", bottom: "18mm", left: "16mm", right: "16mm" };
const PAGE_H_MM = 297;
const mm2px = (mm) => (mm * 96) / 25.4;
const CONTENT_H_PX = mm2px(
  PAGE_H_MM - parseFloat(MARGIN.top) - parseFloat(MARGIN.bottom)
);

fs.mkdirSync(OUT, { recursive: true });
fs.mkdirSync(WORK, { recursive: true });

const css = fs.readFileSync(path.join(DOCS, "_pdf-theme.css"), "utf8");
const js = fs.readFileSync(path.join(DOCS, "_pdf-toc.js"), "utf8");
const hlTheme = fs.readFileSync(
  path.join(HERE, "node_modules/highlight.js/styles/atom-one-dark.min.css"),
  "utf8"
);

const TITLES = {
  "00-README": ["EMS Documentation Index", "How to read this guide and the vocabulary it uses"],
  "01-foundations": ["Foundations", "Layers, the request lifecycle, the database, and configuration"],
  "02-authentication": ["Authentication", "JWT, the security filter chain, and the four auth flows"],
  "03-employees": ["Employees", "The employee directory, endpoint by endpoint and flow by flow"],
  "04-departments": ["Departments", "Department CRUD, the details view, and the delete guard"],
  "05-attendance": ["Attendance", "Check-in, check-out, search, and the self-service endpoint"],
  "06-frontend": ["The Frontend", "Routing, stores, TanStack Query, and every page"],
  "07-security-model": ["The Security Model", "Roles, the endpoint permission matrix, and the weak spots"],
  "08-errors-and-glossary": ["Errors, Flows, Glossary", "Status codes, all 18 flows, and what is still missing"],
  "09-round-trip-traces": ["End-to-End Round Trips", "Every flow traced in both directions, browser to database and back"],
};

const SUBJECTS = {
  "00-README": "Index and glossary",
  "01-foundations": "Architecture and configuration",
  "02-authentication": "Login, register, session, logout",
  "03-employees": "Employee management flows",
  "04-departments": "Department management flows",
  "05-attendance": "Attendance flows and status rules",
  "06-frontend": "React architecture",
  "07-security-model": "Authorization rules",
  "08-errors-and-glossary": "Error handling and reference",
  "09-round-trip-traces": "Full request and response traces",
};

marked.use({ gfm: true, breaks: false });

/* marked escapes these inside <pre><code> */
function decodeEntities(s) {
  return s
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, "&");
}

/* Highlight in Node so no browser bundle has to be built or inlined. */
function highlightCode(html) {
  return html.replace(
    /<pre><code(?: class="language-([\w+#-]+)")?>([\s\S]*?)<\/code><\/pre>/g,
    (_m, lang, body) => {
      const text = decodeEntities(body);
      const cls = `language-${lang || "text"}`;
      let value = null;
      try {
        value =
          lang && hljs.getLanguage(lang)
            ? hljs.highlight(text, { language: lang }).value
            : hljs.highlightAuto(text).value;
      } catch {
        value = null;
      }
      if (value === null) {
        return `<pre data-lang="${lang || "text"}"><code class="${cls}">${body}</code></pre>`;
      }
      return `<pre data-lang="${lang || "text"}"><code class="${cls} hljs">${value}</code></pre>`;
    }
  );
}

function shell({ title, subtitle, subject, bodyHtml }) {
  const dateStr = new Date().toLocaleDateString("en-GB", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });

  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>${title}</title>
<style>${hlTheme}</style>
<style>${css}</style>
</head>
<body class="has-cover">
<header class="cover">
  <div class="cover-rule"></div>
  <p class="cover-kicker">Employment Management System</p>
  <h1 class="cover-title">${title}</h1>
  <p class="cover-sub">${subtitle}</p>
  <div class="cover-meta">
    <span>${subject}</span>
    <span class="dot"></span>
    <span>${dateStr}</span>
  </div>
  <div class="cover-rule"></div>
</header>
<main class="doc">${bodyHtml}</main>
<script>
window.__PDF__ = ${JSON.stringify({ title, subject, contentH: CONTENT_H_PX })};
</script>
<script>${js}</script>
</body>
</html>`;
}

const norm = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, "");

/* Reads the printed PDF back and reports which page each heading really landed
   on, so the contents page can be corrected. -1 when not found. */
async function pagesOfHeadings(pdfPath, rows) {
  const pdfjs = await import("pdfjs-dist/legacy/build/pdf.mjs");
  const doc = await pdfjs.getDocument({
    data: new Uint8Array(fs.readFileSync(pdfPath)),
  }).promise;

  const pageText = [];
  for (let i = 1; i <= doc.numPages; i++) {
    const tc = await (await doc.getPage(i)).getTextContent();
    pageText.push(norm(tc.items.map((it) => it.str).join(" ")));
  }

  return rows.map((row) => {
    const needle = norm(row.text).slice(0, 34);
    if (needle.length < 6) return -1;
    /* skip the cover, and the contents page which repeats every heading */
    for (let i = 2; i < pageText.length; i++) {
      if (pageText[i].includes(needle)) return i + 1;
    }
    return -1;
  });
}

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: "new",
  args: ["--no-sandbox", "--disable-dev-shm-usage", "--font-render-hinting=none"],
});

const files = fs
  .readdirSync(DOCS)
  .filter((f) => f.endsWith(".md"))
  .sort();

const built = [];

for (const file of files) {
  const key = file.replace(/\.md$/, "");
  const [title, subtitle] = TITLES[key] ?? [key.replace(/^\d+-/, "").replace(/-/g, " "), ""];
  const subject = SUBJECTS[key] ?? "";
  const md = fs.readFileSync(path.join(DOCS, file), "utf8");

  let html = highlightCode(marked.parse(md));

  /* drop the source document's own H1 - the cover already carries the title */
  html = html.replace(/^\s*<h1[^>]*>[\s\S]*?<\/h1>\s*/, "");

  const htmlPath = path.join(WORK, `${key}.html`);
  fs.writeFileSync(
    htmlPath,
    shell({ title, subtitle, subject, bodyHtml: html })
  );

  const page = await browser.newPage();
  await page.goto(`file:///${htmlPath.replace(/\\/g, "/")}`, {
    waitUntil: "networkidle0",
  });
  await page.evaluate(() => document.fonts.ready);

  const audit = await page.evaluate(() => ({
    h2: document.querySelectorAll("h2").length,
    toc: document.querySelectorAll(".toc li").length,
    broken: Array.from(document.querySelectorAll(".toc li a")).filter(
      (a) => !document.getElementById(a.getAttribute("href").slice(1))
    ).length,
    highlightedSpans: document.querySelectorAll("code.hljs span").length,
    codeBlocks: document.querySelectorAll("pre[data-lang]").length,
    unlabelled: document.querySelectorAll("pre:not([data-lang])").length,
    dupIds: (() => {
      const ids = Array.from(document.querySelectorAll("[id]")).map((e) => e.id);
      return ids.length - new Set(ids).size;
    })(),
    tocRows: Array.from(document.querySelectorAll(".toc li")).map((li) => ({
      text: li.querySelector(".t-txt").textContent.trim(),
      page: Number(li.querySelector(".t-page").textContent),
    })),
  }));

  if (audit.broken || audit.dupIds || audit.unlabelled) {
    throw new Error(
      `${key}: ${audit.broken} broken TOC anchors, ${audit.dupIds} duplicate ids, ` +
        `${audit.unlabelled} code blocks with no language label`
    );
  }

  const pdfPath = path.join(OUT, `${key}.pdf`);
  const printOptions = {
    path: pdfPath,
    format: "A4",
    printBackground: true,
    displayHeaderFooter: true,
    headerTemplate: `<div style="font-size:7px;width:100%;padding:0 16mm;color:#94a3b8;display:flex;justify-content:space-between;"><span>${subject}</span><span>EMS</span></div>`,
    footerTemplate: `<div style="font-size:7.5px;width:100%;padding:0 16mm;color:#94a3b8;display:flex;justify-content:space-between;align-items:center;"><span>${title}</span><span class="pageNumber"></span></div>`,
    margin: MARGIN,
  };

  /* Pass 1 prints the page numbers the DOM predicted. Break-avoidance rules
     (a heading may not be orphaned at a page foot, a table row may not split)
     can push a block onto the next page, which the DOM cannot predict. So read
     the real pagination back out of the printed PDF and correct the numbers.
     Only the digits change, so the layout - and therefore the pagination -
     is unchanged, and one correction pass is enough. */
  await page.pdf(printOptions);

  const printed = await page.evaluate(() =>
    Array.from(document.querySelectorAll(".toc li .t-page")).map((n) => n.textContent)
  );
  const measured = await pagesOfHeadings(pdfPath, audit.tocRows);

  if (measured.some((m, i) => m !== -1 && String(m) !== printed[i])) {
    await page.evaluate((rows) => {
      document.querySelectorAll(".toc li .t-page").forEach((n, i) => {
        n.textContent = String(rows[i]);
      });
    }, measured);
    await page.pdf(printOptions);
  }

  const toc = audit.tocRows.map((r, i) => ({
    ...r,
    page: measured[i] > 0 ? measured[i] : r.page,
  }));
  const bytes = fs.statSync(pdfPath).size;
  built.push({ key, title, subject, bytes, toc });

  const corrected = toc.filter((r, i) => r.page !== audit.tocRows[i].page).length;
  console.log(
    `${key}.pdf`.padEnd(28) +
      `${(bytes / 1024).toFixed(0).padStart(5)} KB   ` +
      `sections=${String(audit.toc).padStart(2)}  ` +
      `code=${String(audit.codeBlocks).padStart(2)}/${String(audit.highlightedSpans).padStart(4)}` +
      (corrected ? `  (${corrected} contents numbers corrected)` : "")
  );

  await page.close();
}

await browser.close();

/* record the real page counts for the verifier */
const { PDFDocument } = await import("pdf-lib");
for (const b of built) {
  const doc = await PDFDocument.load(fs.readFileSync(path.join(OUT, `${b.key}.pdf`)), {
    updateMetadata: false,
  });
  b.pages = doc.getPageCount();
}
fs.writeFileSync(
  path.join(OUT, "_manifest.json"),
  JSON.stringify({ generated: new Date().toISOString(), documents: built }, null, 2)
);

console.log(
  `\n${built.length} PDFs in ${OUT}  (${built.reduce((n, b) => n + b.pages, 0)} pages)`
);