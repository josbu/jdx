package cn.yiidii.jdx.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.yiidii.jdx.config.prop.SystemConfigProperties;
import cn.yiidii.jdx.config.prop.SystemConfigProperties.QLConfig;
import cn.yiidii.jdx.model.dto.AdminNotifyEvent;
import cn.yiidii.jdx.support.ITask;
import cn.yiidii.jdx.util.JDXUtil;
import cn.yiidii.jdx.util.ScheduleTaskUtil;
import cn.yiidii.jdx.util.WXPushUtil;
import cn.yiidii.jdx.util.jd.JDTaskUtil;
import cn.yiidii.jdx.util.jd.JDTaskUtil.CheckCookieResult;
import com.alibaba.fastjson.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * JDTaskService
 *
 * @author ed w
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JDTaskService implements ITask {

//    private static Long cfdNextTime = 0L;
//    private static Long offsetTime = 1L;
//    private static Long advanceTime = -20L;

    private final SystemConfigProperties systemConfigProperties;
    private final QLService qlService;
    private final ScheduleTaskUtil scheduleTaskUtil;

//    @PostConstruct
//    public void init() {
//        cfdNextTime = DateUtil.beginOfHour(DateUtil.offsetHour(new Date(), 0)).toJdkDate().getTime();
//        log.debug(StrUtil.format("初始化财富岛下次时间为: {}", DateUtil.formatDateTime(new Date(cfdNextTime))));
//    }

    public List<JSONObject> timerCheckCookie() {
        // 所有青龙节点
        List<QLConfig> qlConfigs = systemConfigProperties.getQls();
        ThreadPoolTaskExecutor asyncExecutor = SpringUtil.getBean("asyncExecutor", ThreadPoolTaskExecutor.class);
        // 最终结果
        List<JSONObject> result = new ArrayList<>();
        for (QLConfig qlConfig : qlConfigs) {
            // 单个QL下的可用的Cookie
            List<JSONObject> envs = qlService.searchEnv(qlConfig, "JD_COOKIE")
                    .stream().filter(e -> e.getInteger("status") == 0).collect(Collectors.toList());
            // 一个Cookie一个任务
            List<CompletableFuture<CheckCookieResult>> completableFutures = envs.stream().map(env -> CompletableFuture.supplyAsync(() -> {
                String value = env.getString("value");
                Thread.currentThread().setName(StrUtil.format("checkCookie_{}", JDXUtil.getPtPinFromCK(value)));
                CheckCookieResult checkCookieResult = JDTaskUtil.checkCookie(value);
                checkCookieResult.setId(env.getString("id"));
                if (checkCookieResult.isExpired()) {
                    // 通知到微信
                    String remarks = env.getString("remarks");
                    String uid = JDXUtil.getUidFromRemark(remarks);
                    if (StrUtil.isBlank(uid)) {
                        return checkCookieResult;
                    }
                    WXPushUtil.send(systemConfigProperties.getWxPusherAppToken(),
                            Arrays.asList(uid),
                            "Cookie失效通知",
                            StrUtil.format("{}, {}", checkCookieResult.getPtPin(), checkCookieResult.getRemark()),
                            "1");
                }
                return checkCookieResult;
            }, asyncExecutor)).collect(Collectors.toList());
            // 等待所有任务执行完成
            List<CheckCookieResult> checkCookieResults = completableFutures.stream().map(CompletableFuture::join).collect(Collectors.toList());
            List<String> expiredPtPins = checkCookieResults.stream()
                    .filter(CheckCookieResult::isExpired)
                    .map(CheckCookieResult::getPtPin)
                    .collect(Collectors.toList());
            List<String> ids = checkCookieResults.stream().filter(CheckCookieResult::isExpired).map(CheckCookieResult::getId).collect(Collectors.toList());
            try {
                // 禁用Cookie
                qlService.disableEnv(qlConfig, ids);
            } catch (Exception e) {
                log.error("定时检查cookie时, 禁用环境变量发生异常, displayName: {}", qlConfig.getDisplayName());
            }
            if (CollUtil.isNotEmpty(expiredPtPins)) {
                JSONObject jo = new JSONObject();
                jo.put("displayName", qlConfig.getDisplayName());
                jo.put("expiredPtPins", expiredPtPins);
                result.add(jo);
            }
        }

        String adminUid = systemConfigProperties.getWxPusherAdminUid();
        if (CollUtil.isNotEmpty(result) && StrUtil.isNotBlank(adminUid)) {
            String adminContent = result.stream().map(jo ->
                    StrUtil.format("节点【{}】以下Cookie已失效，已自动禁用\r\n{}",
                            jo.getString("displayName"),
                            CollUtil.join(jo.getJSONArray("expiredPtPins"), "\r\n")))
                    .collect(Collectors.joining("\r\n\r\n"));
            SpringUtil.publishEvent(new AdminNotifyEvent("Cookie失效通知", adminContent));
        }
        return result;
    }

//    public void exchangeCfd() {
//        // 所有QL节点
//        List<QLConfig> qlConfigs = systemConfigProperties.getQls();
//        // 所有env
//        List<JSONObject> allEnvs = qlConfigs.stream()
//                .flatMap(qlConfig -> qlService.searchEnv(qlConfig, "JDX_CFD_COOKIE").stream().filter(e -> e.getInteger("status") == 0).peek(env -> env.put("qlDisplayName", qlConfig.getDisplayName())))
//                .collect(Collectors.toList());
//        ThreadPoolTaskExecutor asyncExecutor = SpringUtil.getBean("asyncExecutor", ThreadPoolTaskExecutor.class);
//        List<CompletableFuture<CfdExchangeResult>> futures = allEnvs.stream().map(env -> CompletableFuture.supplyAsync(() -> {
//            String ck = env.getString("value");
//            // 检测ck有效性
//            CheckCookieResult checkCookieResult;
//            try {
//                checkCookieResult = JDTaskUtil.checkCookie(ck);
//                if (checkCookieResult.isExpired()) {
//                    throw new BizException("cookie已失效");
//                }
//            } catch (Exception e) {
//                return new CfdExchangeResult().setCookie(ck).setPtPin("").setResult(StrUtil.format("[兑换财富岛红包] 发生异常: {}", e.getMessage()));
//            }
//            // 开始兑换
//            try {
//                while (System.currentTimeMillis() < cfdNextTime + advanceTime) {
//                    // 空转
//                }
//                HttpResponse response = HttpRequest.get(JDTaskUtil.getCfdUrl(ck))
//                        .cookie(ck)
//                        .header(Header.REFERER, "https://st.jingxi.com/")
//                        .header(Header.USER_AGENT, "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1")
//                        .execute();
//                log.debug(StrUtil.format("[兑换财富岛红包] pt_pin: {}, 响应: {}", checkCookieResult.getPtPin(), response.body()));
//                JSONObject responseJo = JSON.parseObject(response.body());
//                Integer iRet = responseJo.getInteger("iRet");
//                String r;
//                if (iRet == 0) {
//                    r = "抢到了";
//                } else {
//                    r = responseJo.getString("sErrMsg");
//                }
//                log.debug(StrUtil.format("[兑换财富岛红包], pt_pin: {}, 最终结果: {}", r));
//                return new CfdExchangeResult().setPtPin(checkCookieResult.getPtPin()).setResult(r);
//            } catch (Exception e) {
//                e.printStackTrace();
//                return new CfdExchangeResult().setPtPin(checkCookieResult.getPtPin()).setResult(StrUtil.format("发生异常: {}", e.getMessage()));
//            }
//        }, asyncExecutor)).collect(Collectors.toList());
//        List<CfdExchangeResult> allResult = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
//        log.error(JSONObject.toJSONString(allResult));
//    }

    @Override
    public void startTimerTask() {
        scheduleTaskUtil.startCron("jdTask_checkCookie", this::timerCheckCookie, "0 0 12 * * ?");
    }
}
