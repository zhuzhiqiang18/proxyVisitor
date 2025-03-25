package org.example;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Slf4j
public class PlaywrightUtil {
    public static ThreadLocal<Browser> browserThreadLocal = new ThreadLocal<>();



    public static void openBrowser(){

    }


    public static   String getHtml(String url, Proxy proxy){

//        if (browserThreadLocal.get()!=null){
//            return visit(url,browserThreadLocal.get().newPage());
//        }

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();
        options.setHeadless(false);
        options.setArgs(Arrays.asList("--no-sandbox", "--disable-gpu", "--disable-setuid-sandbox", "--disable-dev-shm-usage"));
        options.setSlowMo(50);
      //  options.setProxy(proxy);
        try (Playwright playwright = Playwright.create()){
            AtomicReference<Browser> browser = new AtomicReference<>(playwright.chromium().launch(options));
            BrowserContext context = browser.get().newContext();
            AtomicReference<Page> page = new AtomicReference<>(context.newPage());
            Consumer<Browser> browserCrashConsumer = errBrowser -> {
                log.error("浏览器崩溃");
                errBrowser.close();
                browser.set(playwright.chromium().launch(options));
            };
            Consumer<Page> pageCrashConsumer = errPage -> {
                log.error("页面崩溃");
                errPage.close();
                page.set(context.newPage());
            };
            browser.get().onDisconnected(browserCrashConsumer);
            page.get().onCrash(pageCrashConsumer);

            String html =  visit(url, page.get());

            // 导出完成移除监听事件
            page.get().offCrash(pageCrashConsumer);
            browser.get().offDisconnected(browserCrashConsumer);
            // 关闭页面 退出浏览器
            page.get().close();
            browser.get().close();
            browserThreadLocal.set(browser.get());
            return html;
        }finally {
        }
    }



    private static String visit(String url, Page page) {

        // 设置默认超时时间为60s
        page.setDefaultNavigationTimeout(30000);
        // 设置视图大小为800*600
        page.setViewportSize(800, 600);
        page.navigate(url);
        // 等待网络空闲  且所有的图片都加载完成
        page.waitForLoadState(LoadState.NETWORKIDLE);
        //page.waitForFunction("Array.from(document.images).every(img => img.complete)");


        //是否需要截图
//        ScreenOption threadLocal = WebElementClustering.screenOptionThreadLocal.get();
//        if (threadLocal!=null && threadLocal.isScreen()){
//            String imgPath = "/doaminScreen/"+ UUID.randomUUID()+".png";
//            threadLocal.setFilePath(imgPath);
//            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(imgPath)));
//        }

        //滑动页面
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");

        page.waitForTimeout(2000);



        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
        page.waitForTimeout(2000);



        String html =  page.content();

        return html;

    }

    public  void close(){
        try {
            Browser browser = browserThreadLocal.get();
            if (browser!=null){
                browser.close();
            }

        }catch (Exception e){

        }finally {
            browserThreadLocal.remove();
        }

    }



}
