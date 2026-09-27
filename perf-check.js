const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const root = 'file:///C:/Users/Mohammed%20Hassan%20Nuri/OneDrive/Desktop/ProStudyWeb/';
  const pages = [
    'index.html',
    'dashboard.html',
    'news.html',
    'homework.html',
    'request.html',
    'quickquiz.html',
    'about.html'
  ];

  const results = [];

  for (const pagePath of pages) {
    const page = await browser.newPage();
    const url = root + pagePath;
    const start = Date.now();

    await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 30000 });
    await page.waitForTimeout(800);

    const metrics = await page.evaluate(() => {
      const nav = performance.getEntriesByType('navigation')[0] || {};
      const resources = performance.getEntriesByType('resource');
      const paint = performance.getEntriesByType('paint');
      const totalTransferBytes = resources.reduce((sum, entry) => sum + (entry.transferSize || 0), 0);

      return {
        domContentLoaded: nav.domContentLoadedEventEnd ? nav.domContentLoadedEventEnd - nav.startTime : null,
        load: nav.loadEventEnd ? nav.loadEventEnd - nav.startTime : null,
        firstPaint: paint.find((entry) => entry.name === 'first-paint')?.startTime ?? null,
        firstContentfulPaint: paint.find((entry) => entry.name === 'first-contentful-paint')?.startTime ?? null,
        resourceCount: resources.length,
        transferBytes: totalTransferBytes
      };
    });

    results.push({
      page: pagePath,
      elapsedMs: Date.now() - start,
      ...metrics
    });

    await page.close();
  }

  console.log(JSON.stringify(results, null, 2));
  await browser.close();
})();
