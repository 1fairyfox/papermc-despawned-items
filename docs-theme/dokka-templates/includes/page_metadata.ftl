<#-- fairyfox docs-site chrome: head bundle (chrome VERSION in docs-theme/chrome/VERSION).
     Verbatim copy of chrome/head.html — theme-color metas, the pre-paint reader script,
     the shared fonts, and the vendored master stylesheet — plus Dokka's own <title>/favicon.
     Only the CSS href is filled: it points at the vendored copy of main.css at the site root. -->
<#macro display>
    <title>${pageName}</title>
    <@template_cmd name="pathToRoot">
    <link href="${pathToRoot}images/logo-icon.svg" rel="icon" type="image/svg">
    </@template_cmd>

    <#-- 1 · Browser-chrome colour, per scheme (identical everywhere) -->
    <meta name="theme-color" content="#ef6149" media="(prefers-color-scheme: light)">
    <meta name="theme-color" content="#191116" media="(prefers-color-scheme: dark)">

    <#-- 2 · Reader pre-paint: apply saved theme / accent / text vars before first paint,
         from the origin-wide fairyfox:reader:b key, so there's no flash and the choice
         carries across the mesh. Copied verbatim from chrome/head.html. -->
    <script>
    (function(){try{var p=JSON.parse(localStorage.getItem("fairyfox:reader:b")||"{}"),r=document.documentElement,
    S=[15,16.5,18,20,22],L={tight:1.5,normal:1.65,relaxed:1.9},W={narrow:"38rem",normal:"46rem",wide:"58rem"};
    if(p.theme&&p.theme!=="system")r.setAttribute("data-theme",p.theme);
    if(p.size!=null)r.style.fontSize=S[Math.max(0,Math.min(S.length-1,p.size|0))]+"px";
    var story=r.hasAttribute("data-read")||r.hasAttribute("data-story");
    if(story&&p.lh)r.style.setProperty("--reading-lh",String(L[p.lh]||L.normal));
    if(story&&p.width)r.style.setProperty("--reading-width",W[p.width]||W.normal);
    if(p.accent){var h=p.accent,ink="color-mix(in srgb, "+h+", var(--text) 42%)";
    r.style.setProperty("--accent",h);r.style.setProperty("--violet",h);
    r.style.setProperty("--violet-deep","color-mix(in srgb, "+h+", #000 12%)");
    r.style.setProperty("--accent-ink",ink);r.style.setProperty("--link",ink);
    r.style.setProperty("--link-hover","color-mix(in srgb, "+h+", var(--text) 26%)");
    r.style.setProperty("--glow","color-mix(in srgb, "+h+" 40%, transparent)");}}catch(e){}})();
    </script>

    <#-- 3 · Fonts — same families/weights + preconnect/display=swap so type doesn't reflow -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,500;9..144,600;9..144,700&family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet">

    <#-- 4 · The master stylesheet — vendored copy of fairyfox.io's main.css at the site root. -->
    <@template_cmd name="pathToRoot">
    <link rel="stylesheet" href="${pathToRoot}main.css">
    </@template_cmd>
</#macro>
