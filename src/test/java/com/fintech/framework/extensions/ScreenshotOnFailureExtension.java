package com.fintech.framework.extensions;

import com.fintech.framework.base.UiTestBase;
import com.fintech.framework.utils.ReportArtifacts;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

public final class ScreenshotOnFailureExtension implements TestWatcher {
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        Page page = UiTestBase.currentPage();
        if (page == null || page.isClosed()) {
            return;
        }

        page.screenshot(new Page.ScreenshotOptions()
                .setFullPage(true)
                .setPath(ReportArtifacts.screenshotPath(context.getDisplayName())));
    }
}
