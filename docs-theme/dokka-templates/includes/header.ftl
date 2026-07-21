<#-- fairyfox docs-site chrome: header + subnav (chrome VERSION in docs-theme/chrome/VERSION).
     The shared masthead (chrome/header.html) and the section row (chrome/subnav.html) are
     copied verbatim except the fixed slots: the primary "Projects" item is marked active
     (docs-site standard 05 — a standalone sub-project is ALWAYS under Projects, never Docs),
     and {{FF_SUBNAV_ITEMS}} is this project's canonical three-zone section row
     ([name=overview] · API · Download · [Repository ↗] — only pages that really exist, every
     centre link a chrome-wearing page). Dokka's own reference bar is kept below as the API
     reference's controls (search / theme / source-set filter). -->
<#import "source_set_selector.ftl" as source_set_selector>
<#macro display>
    <header class="site-header">
      <div class="wrap">
        <a class="brand" href="https://fairyfox.io/">
          <@template_cmd name="pathToRoot">
          <img class="brand-logo" src="${pathToRoot}fox.png" alt="" aria-hidden="true">
          </@template_cmd>
          <span class="brand-name">Fairy&nbsp;Fox</span>
        </a>

        <button class="nav-toggle" aria-label="Toggle navigation" aria-expanded="false">
          <span></span><span></span><span></span>
        </button>

        <nav class="nav" aria-label="Primary">
          <a href="https://fairyfox.io/">Home</a>
          <a href="https://fairyfox.io/projects/" class="active" aria-current="page">Projects</a>
          <details class="dd">
            <summary>Farms</summary>
            <div class="dd-panel">
              <div class="dd-group">
                <div class="dd-links">
                  <a href="https://fairyfox.io/stories/">Stories</a>
                  <a href="https://fairyfox.io/games/">Games</a>
                </div>
              </div>
            </div>
          </details>
          <a href="https://fairyfox.io/docs/">Docs</a>
          <a href="https://fairyfox.io/blog/">Updates</a>
          <a href="https://fairyfox.io/about/">About</a>
        </nav>
      </div>
    </header>

    <#-- Canonical three-zone subnav (docs-site 05). The Dokka tree is boundaried under
         /api/, so ${'$'}{pathToRoot} reaches the API root and ../ from there is the site
         root. API is the current section on every generated page. -->
    <nav class="subnav" aria-label="Section">
      <div class="wrap">
        <@template_cmd name="pathToRoot">
        <a href="${pathToRoot}../index.html" class="subnav-home">PaperMC Despawned Items</a>
        <a href="${pathToRoot}../notes/index.html">Notes</a>
        <a href="${pathToRoot}../tutorials.html">Tutorials</a>
        <a href="${pathToRoot}../changelog.html">Changelog</a>
        <a href="${pathToRoot}index.html" class="active" aria-current="page">API</a>
        <a href="${pathToRoot}../downloads.html">Download</a>
        </@template_cmd>
        <a href="https://github.com/1fairyfox/papermc-despawned-items" class="subnav-repo">Repository ↗</a>
      </div>
    </nav>

    <#-- Dokka's own reference bar: the API-reference controls live here (search, theme
         toggle, source-set filter). The way home is always the shared masthead above. -->
    <header class="navigation theme-dark" id="navigation-wrapper" role="banner">
        <@template_cmd name="pathToRoot">
            <a class="library-name--link" href="${pathToRoot}index.html" tabindex="1">
                <@template_cmd name="projectName">
                    ${projectName}
                </@template_cmd>
            </a>
        </@template_cmd>
        <button class="navigation-controls--btn navigation-controls--btn_toc ui-kit_mobile-only" id="toc-toggle"
                type="button">Toggle table of contents
        </button>
        <div class="navigation-controls--break ui-kit_mobile-only"></div>
        <div class="library-version" id="library-version">
            <#-- This can be handled by the versioning plugin -->
            <@version/>
        </div>
        <div class="navigation-controls">
            <@source_set_selector.display/>
            <#if homepageLink?has_content>
                <a class="navigation-controls--btn navigation-controls--btn_homepage" id="homepage-link"
                   href="${homepageLink}"></a>
            </#if>
            <button class="navigation-controls--btn navigation-controls--btn_theme" id="theme-toggle-button"
                    type="button">Switch theme
            </button>
            <div class="navigation-controls--btn navigation-controls--btn_search" id="searchBar" role="button">Search in
                API
            </div>
        </div>
    </header>
</#macro>
