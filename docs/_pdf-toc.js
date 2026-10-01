/* Post-process the rendered markdown inside the page.
   Runs after marked + highlight.js have produced the DOM, before Chrome prints it. */
(function () {
  var meta = window.__PDF__ || {};

  /* ---------- stable, unique ids on every heading ---------- */
  var used = Object.create(null);
  function slugify(text) {
    var base =
      text
        .toLowerCase()
        .replace(/`/g, "")
        .replace(/[^\w\s-]/g, "")
        .trim()
        .replace(/\s+/g, "-")
        .slice(0, 60) || "section";
    var id = base;
    var n = 2;
    while (used[id]) id = base + "-" + n++;
    used[id] = true;
    return id;
  }
  document.querySelectorAll("h2, h3").forEach(function (h) {
    if (!h.id) h.id = slugify(h.textContent);
  });

  /* ---------- tables: make sure there is a repeating header row ---------- */
  document.querySelectorAll("table").forEach(function (t) {
    if (t.querySelector("thead")) return;
    var firstRow = t.querySelector("tr");
    if (!firstRow) return;
    var th = document.createElement("thead");
    th.appendChild(firstRow.cloneNode(true));
    t.insertBefore(th, t.firstChild);
    firstRow.remove();
  });

  /* ---------- callouts: a blockquote opening with a bold keyword ---------- */
  var TONE = {
    NOTE: "info",
    TIP: "tip",
    IMPORTANT: "important",
    WARNING: "warn",
    CRITICAL: "critical",
    GOTCHA: "gotcha",
  };
  document.querySelectorAll("blockquote").forEach(function (bq) {
    var first = bq.querySelector("strong");
    var word = first
      ? first.textContent.trim().replace(/[:\s]+$/, "").toUpperCase()
      : "";
    bq.classList.add("callout");
    if (TONE[word]) {
      bq.classList.add("callout-" + TONE[word]);
      first.textContent = first.textContent.replace(/[:\s]+$/, "");
    } else {
      bq.classList.add("callout-quote");
    }
  });

  /* ---------- external links ---------- */
  document.querySelectorAll('a[href^="http"]').forEach(function (a) {
    a.setAttribute("target", "_blank");
    a.setAttribute("rel", "noopener");
  });

  /* ---------- table of contents, with real page numbers ----------
     Page breaks are vertical flow of exactly `contentH` pixels per page
     (build.mjs passes the page.pdf() content height in), EXCEPT at points
     where the stylesheet forces a break - after the cover, after the
     contents, and anywhere marked .section-break. So the page a heading
     lands on is found from the nearest forced break above it. */
  var contentH = meta.contentH || 979;
  var main = document.querySelector("main.doc");
  var sections = Array.prototype.slice.call(main.querySelectorAll("h2"));

  function pageMap() {
    var starts = [0];
    var all = document.body.querySelectorAll("*");
    for (var i = 0; i < all.length; i++) {
      var cs = window.getComputedStyle(all[i]);
      var after =
        cs.breakAfter === "page" || cs.pageBreakAfter === "always";
      var before = cs.breakBefore === "page" || cs.pageBreakBefore === "always";
      if (!after && !before) continue;
      var r = all[i].getBoundingClientRect();
      starts.push(
        (after ? r.bottom : r.top) + window.pageYOffset
      );
    }
    starts.sort(function (a, b) {
      return a - b;
    });
    return function (y) {
      var idx = 0;
      for (var k = 0; k < starts.length; k++) {
        if (y >= starts[k] - 0.5) idx = k;
      }
      return idx + 1 + Math.floor((y - starts[idx]) / contentH);
    };
  }

  if (sections.length) {
    var nav = document.createElement("nav");
    nav.className = "toc";
    nav.innerHTML =
      '<h2 class="toc-h">Contents</h2><ol>' +
      sections
        .map(function (h, i) {
          return (
            '<li><a href="#' +
            h.id +
            '"><span class="t-num">' +
            String(i + 1).padStart(2, "0") +
            '</span><span class="t-txt">' +
            h.innerHTML +
            '</span><span class="t-page"></span></a></li>'
          );
        })
        .join("") +
      "</ol>";
    main.insertBefore(nav, main.firstChild);

    /* the contents block is itself a forced break, so measure after insert */
    var pageOf = pageMap();

    sections.forEach(function (h) {
      var page = pageOf(h.getBoundingClientRect().top + window.pageYOffset);
      var target = nav.querySelector('a[href="#' + h.id + '"] .t-page');
      if (target) target.textContent = String(page);
    });
  }
})();