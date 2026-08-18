package com.fintech.framework.base;

import com.fintech.framework.extensions.ScreenshotOnFailureExtension;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ScreenshotOnFailureExtension.class)
public abstract class UiTestBase extends FintechTestBase {
    private static final ThreadLocal<Page> CURRENT_PAGE = new ThreadLocal<>();

    protected Browser browser;
    protected BrowserContext browserContext;
    protected Page page;

    @BeforeEach
    void createPage() {
        BrowserType browserType = switch (config.browser()) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };

        browser = browserType.launch(new BrowserType.LaunchOptions().setHeadless(config.headless()));
        browserContext = browser.newContext(new Browser.NewContextOptions().setBaseURL(baseUrl));
        page = browserContext.newPage();
        page.setDefaultTimeout(config.uiDefaultTimeoutMs());
        CURRENT_PAGE.set(page);
    }

    @AfterEach
    void closePage() {
        CURRENT_PAGE.remove();
        if (browserContext != null) {
            browserContext.close();
        }
        if (browser != null) {
            browser.close();
        }
    }

    public static Page currentPage() {
        return CURRENT_PAGE.get();
    }
}
