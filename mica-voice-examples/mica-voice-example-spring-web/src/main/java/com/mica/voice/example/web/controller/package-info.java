/**
 * mica-voice Spring Boot Web 示例。
 *
 * <p>展示如何用 {@code mica-voice-spring-boot-starter} 在 Spring Boot 中
 * 一站式启用 ASR / TTS / 声纹等能力：
 * <ul>
 *     <li>{@code application.yml} 中配 {@code mica.voice.*}</li>
 *     <li>直接注入 {@code OfflineAsrService / TtsService / SpeakerService} Bean</li>
 *     <li>提供 REST 上传 wav、GET 合成 wav、声纹注册/验证 等典型接口</li>
 * </ul>
 *
 * @author dreamlu
 */
package com.mica.voice.example.web.controller;
