package com.mail.util;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * HTML 清理器 - 防止 XSS 攻击
 * 保留安全的 HTML 标签，移除脚本和危险属性
 */
@Component
public class HtmlSanitizer {

    private final PolicyFactory policy;

    public HtmlSanitizer() {
        this.policy = new HtmlPolicyBuilder()
                // 允许的文本格式标签
                .allowElements("b", "i", "u", "strong", "em", "br", "p", "div", "span")
                .allowElements("h1", "h2", "h3", "h4", "h5", "h6")
                .allowElements("ul", "ol", "li")
                .allowElements("a")
                .allowAttributes("href").onElements("a")
                .allowUrlProtocols("http", "https", "mailto")
                // 允许的表格标签
                .allowElements("table", "thead", "tbody", "tr", "th", "td")
                .allowAttributes("colspan", "rowspan").onElements("th", "td")
                .allowAttributes("align").onElements("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "td", "th")
                // 允许的图片标签（限制来源）
                .allowElements("img")
                .allowAttributes("src", "alt", "width", "height").onElements("img")
                .allowUrlProtocols("http", "https")
                // 允许基本的样式属性（移除全局 style，防止 CSS 注入）
                .allowAttributes("class").globally()
                // 移除所有事件处理器（onclick, onerror 等）
                .allowStandardUrlProtocols()
                .toFactory();
    }

    /**
     * 清理 HTML 内容，移除危险的脚本和标签
     */
    public String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return policy.sanitize(html);
    }
}
