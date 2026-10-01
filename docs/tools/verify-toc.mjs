/* Confirms that every page number printed in the contents section of each PDF
   is the page its heading actually lands on. Reads the printed PDFs with
   pdf.js, so it validates the artefact rather than the intermediate HTML.
 *
 *   npm run verify      (after npm run build)
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import * as pdfjs from "pdfjs-dist/legacy/build/pdf.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.resolve(HERE, "../pdf");

const manifestPath = path.join(OUT, "_manifest.json");
if (!fs.existsSync(manifestPath)) {
  throw new Error("No _manifest.json - run `npm run build` first.");
}
const manifest = JSON.parse(fs.readFileSync(manifestPath, "utf8"));

const norm = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, "");

let checked = 0;
let wrong = 0;

for (const d of manifest.documents) {
  const pdf = await pdfjs.getDocument({
    data: new Uint8Array(fs.readFileSync(path.join(OUT, `${d.key}.pdf`))),
  }).promise;

  const pages = [];
  for (let i = 1; i <= pdf.numPages; i++) {
    const tc = await (await pdf.getPage(i)).getTextContent();
    pages.push(norm(tc.items.map((it) => it.str).join(" ")));
  }

  const bad = [];
  for (const row of d.toc) {
    const needle = norm(row.text).slice(0, 34);
    if (needle.length < 6) continue;
    let actual = -1;
    for (let i = 2; i < pages.length; i++) {
      if (pages[i].includes(needle)) {
        actual = i + 1;
        break;
      }
    }
    checked++;
    if (actual !== row.page) {
      wrong++;
      bad.push(`    "${row.text.slice(0, 46)}"  contents says ${row.page}, actually ${actual}`);
    }
  }

  const past = d.toc.filter((t) => t.page > pdf.numPages);
  if (past.length) bad.push(`    ${past.length} contents entries point past the last page`);

  console.log(
    `${d.key.padEnd(24)} ${String(pdf.numPages).padStart(3)} pages  ` +
      `${String(d.toc.length).padStart(2)} contents entries  ` +
      (bad.length === 0 ? "all accurate" : `${bad.length} WRONG`)
  );
  bad.forEach((b) => console.log(b));
}

console.log(
  `\n${checked} contents page numbers checked against the printed PDFs, ${wrong} incorrect`
);
process.exit(wrong === 0 ? 0 : 1);