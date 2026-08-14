/**
 * mica-voice 配置体系（POJO + Builder）。
 *
 * <p>所有配置都是不可变友好 + Builder 形式，
 * 既能在纯 Java 里 {@code AsrConfig.builder().modelDirName(...).build()}，
 * 也能在 Spring Boot 里通过 {@code @ConfigurationProperties} 绑定。
 *
 * @author dreamlu
 */
package net.dreamlu.mica.voice.config;