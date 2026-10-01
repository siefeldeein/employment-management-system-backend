/* Layout QA over the printed PDFs, using the text layer. Catches the faults a
   screenshot would show: missing folios, near-empty pages caused by a bad
   break, and headings stranded as the last thing on a page.
 *
 *   npm run qa
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import * as pdfjs from "pdfjs-dist/legacy/build/pdf.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.resolve(HERE, "../pdf");
const norm = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, "");

const manifestPath = path.join(OUT, "_manifest.json");
if (!fs.existsSync(manifestPath)) {
  throw new Error("No _manifest.json - run `npm run build` first.");
}
const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));

/* an interior page must be reasonably full; the final page is allowed to be
   short because a document ends where it ends - but a truly blank trailing
   page still means a bad break, so keep a floor there too */
const MIN_INTERIOR_CHARS = 220;
const MIN_LAST_PAGE_CHARS = 80;

let problems = 0;

for (const d of manifest.documents) {
  const pdf = await pdfjs.getDocument({
    data: new Uint8Array(fs.readFileSync(path.join(OUT, `${d.key}.pdf`))),
  }).promise;

  const issues = [];
  const heads = new Set(d.toc.map((r) => norm(r.text).slice(0, 30)));

  for (let i = 1; i <= pdf.numPages; i++) {
    const tc = await (await pdf.getPage(i)).getTextContent();
    const raw = tc.items.map((it) => it.str).join(" ");
    const flat = norm(raw);
    const isLast = i === pdf.numPages;

    /* the footer carries the folio as a bare number */
    if (!new RegExp(`(^|\\D)${i}(\\D|$)`).test(flat)) {
      issues.push(`p${i}: no page number in the footer`);
    }

    if (i > 2) {
      const min = isLast ? MIN_LAST_PAGE_CHARS : MIN_INTERIOR_CHARS;
      if (flat.length < min) {
        issues.push(
          `p${i}: only ${flat.length} chars${isLast ? " (last page)" : ""} - "${raw.trim().slice(0, 64)}"`
        );
      }
    }

    /* a heading as the final item on a page is orphaned */
    const tail = norm(raw.trim().slice(-60));
    for (const h of heads) {
      if (h.length > 12 && tail.endsWith(h)) {
        issues.push(`p${i}: heading is stranded at the foot of the page`);
      }
    }
  }

  problems += issues.length;
  console.log(
    `${d.key.padEnd(24)} ${String(pdf.numPages).padStart(3)} pages  ` +
      (issues.length === 0 ? "clean" : `${issues.length} issue(s)`)
  );
  issues.forEach((x) => console.log("    " + x));
}

console.log(`\n${problems} layout issue(s)`);
process.exit(problems === 0 ? 0 : 1);