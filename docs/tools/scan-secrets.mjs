/* Scans the generated PDFs' text layer for any value that is currently a
   secret in src/main/resources/application.yaml.
 *
 *   npm run scan
 *
 * The forbidden values are read from application.yaml at run time rather than
 * hard-coded here, so this file can be committed without repeating a secret
 * and the check keeps working if the secret is ever rotated.
 */
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import * as pdfjs from "pdfjs-dist/legacy/build/pdf.mjs";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.resolve(HERE, "../pdf");
const APP_YAML = path.resolve(HERE, "../../src/main/resources/application.yaml");

/* values that are not secret and would only cause false positives */
const BENIGN = new Set([
  "",
  "disabled",
  "jdbc:mysql://localhost:3306/employee_management_system",
  "ems",
  "update",
  "true",
  "false",
  "8080",
]);

function secretsFromYaml(text) {
  const found = new Map();
  const stack = []; // [{ indent, key }] for the enclosing keys

  for (const raw of text.split(/\r?\n/)) {
    const line = raw.replace(/\s+#.*$/, "");
    if (!line.trim()) continue;
    const m = line.match(/^(\s*)([A-Za-z0-9_.-]+):(?:\s*(.*))?$/);
    if (!m) continue;

    const indent = m[1].length;
    const key = m[2];
    const value = (m[3] || "").trim().replace(/^["']|["']$/g, "");

    while (stack.length && stack[stack.length - 1].indent >= indent) stack.pop();
    const full = [...stack.map((s) => s.key), key].join(".");

    if (!value) {
      stack.push({ indent, key });
      continue;
    }

    const sensitive =
      /^jwt\.(?!expiration$)/.test(full) ||
      /^spring\.datasource\.(username|password)$/.test(full);

    /* a bare number is a timeout, a port or a size - never a secret, and the
       token TTL is worth documenting */
    const numeric = /^\d+$/.test(value);

    if (sensitive && !numeric && !BENIGN.has(value) && value.length >= 6) {
      found.set(full, value);
    }
  }
  return [...found.values()];
}

if (!fs.existsSync(APP_YAML)) {
  console.log(`No ${path.relative(process.cwd(), APP_YAML)} - nothing to scan against.`);
  process.exit(0);
}

const secrets = secretsFromYaml(fs.readFileSync(APP_YAML, "utf8"));
if (!secrets.length) {
  console.log("No sensitive values found in application.yaml.");
  process.exit(0);
}
console.log(`Scanning PDFs against ${secrets.length} sensitive value(s) from application.yaml.`);

let hits = 0;
for (const f of fs.readdirSync(OUT).filter((x) => x.endsWith(".pdf")).sort()) {
  const pdf = await pdfjs.getDocument({
    data: new Uint8Array(fs.readFileSync(path.join(OUT, f))),
  }).promise;
  let text = "";
  for (let i = 1; i <= pdf.numPages; i++) {
    const tc = await (await pdf.getPage(i)).getTextContent();
    text += tc.items.map((it) => it.str).join(" ");
  }
  const leaked = secrets.filter((s) => text.includes(s));
  hits += leaked.length;
  console.log(
    `${f.padEnd(30)} ${leaked.length ? "LEAKS " + leaked.length + " value(s)" : "clean"}`
  );
}

console.log(`\n${hits} secret value(s) found in the PDFs`);
process.exit(hits === 0 ? 0 : 1);