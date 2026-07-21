<#-- fairyfox docs-site chrome: footer + behaviour scripts (chrome VERSION in
     docs-theme/chrome/VERSION). Verbatim copy of chrome/footer.html with the FF_* slots
     filled for this project, then the vendored nav.js / reader.js / coins.js loaded from
     the site root. reader.js injects the "Aa" button into .site-header .wrap; coins.js the
     coin button beside it. Dokka's own generated footer is intentionally dropped — the
     shared footer is the only footer (see chrome/adapters/doxygen.md). -->
<#macro display>
    <footer class="site-footer">
      <div class="wrap">
        <div class="footer-brand">
          <a class="brand" href="https://fairyfox.io/">
            <img class="brand-logo" src="https://fairyfox.io/assets/icons/fox.png" alt="" aria-hidden="true">
            <span class="brand-name">Fairy Fox</span>
          </a>
          <p>The project hub and documentation library for Fairy Fox's software work.</p>
        </div>
        <div class="footer-col">
          <h4>Explore</h4>
          <a href="https://fairyfox.io/projects/">Projects</a>
          <a href="https://fairyfox.io/docs/">Documentation</a>
          <a href="https://fairyfox.io/blog/">Updates</a>
          <a href="https://fairyfox.io/about/">About</a>
        </div>
        <div class="footer-col">
          <h4>This project</h4>
          <a href="https://fairyfox.io/papermc-despawned-items/">PaperMC Despawned Items</a>
          <a href="https://github.com/1fairyfox/papermc-despawned-items">Repository ↗</a>
          <a href="https://fairyfox.io/papermc-despawned-items/legal/privacy/">Privacy</a>
          <a href="https://fairyfox.io/papermc-despawned-items/legal/terms/">Terms</a>
          <a href="https://fairyfox.io/papermc-despawned-items/legal/cookies/">Cookies</a>
        </div>
        <div class="footer-col">
          <h4>Elsewhere</h4>
          <a href="https://github.com/1fairyfox">GitHub ↗</a>
          <a href="https://fairyfox.io/feed.xml">Atom feed</a>
        </div>
      </div>
      <div class="footer-bar">
        <div class="wrap">
          <span>© Fairy Fox</span>
          <span>Built with the fairyfox docs-site standard</span>
          <span class="spacer"></span>
          <a href="https://github.com/1fairyfox">@1fairyfox</a>
        </div>
      </div>
    </footer>

    <@template_cmd name="pathToRoot">
    <script src="${pathToRoot}nav.js" defer></script>
    <script src="${pathToRoot}reader.js" defer></script>
    <script src="${pathToRoot}coins.js" defer></script>
    </@template_cmd>
</#macro>
