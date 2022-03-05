package cn.yiidii.jdx.util.jd;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.yiidii.jdx.util.JDXUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * CheckCk
 *
 * @author ed w
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class JDTaskUtil {

    public CheckCookieResult checkCookie(String cookie) {
        boolean expired = false;
        String ptPin = JDXUtil.getPtPinFromCK(cookie);
        String nickName = ptPin;
        String remark = "Cookie有效";
        boolean useInterface2 = false;

        HttpResponse response;
        JSONObject respJo;
        try {
            response = HttpRequest.get("https://me-api.jd.com/user_new/info/GetJDUserInfoUnion")
                    .cookie(cookie)
                    .execute();
            respJo = JSONObject.parseObject(response.body());
            log.debug(StrUtil.format("[检查Cookie], pt_pin: {}, 接口1响应: {}", ptPin, respJo));
            if (CollUtil.isNotEmpty(respJo)) {
                String retCode = respJo.getString("retcode");
                if (StrUtil.equals("1001", retCode)) {
                    expired = true;
                    remark = "Cookie已失效";
                } else if (StrUtil.equals("0", retCode)) {
                    JSONObject userInfo = respJo.getJSONObject("data").getJSONObject("userInfo");
                    if (CollUtil.isNotEmpty(userInfo)) {
                        nickName = userInfo.getJSONObject("baseInfo").getString("nickname");
                    }
                } else {
                    // 未知状态
                    useInterface2 = true;
                    remark = "JD返回未知状态";
                }
            } else {
                // 京东接口返回空数据
                useInterface2 = true;
                remark = "京东接口返回空数据";
            }

        } catch (Exception e) {
            useInterface2 = true;
            log.debug(StrUtil.format("[检查Cookie], pt_pin: {}, 接口q响应异常, e: {}", ptPin, e.getMessage()));
        }

        if (useInterface2) {
            log.debug(StrUtil.format("[检查Cookie], 继续采用接口2, pt_pin: {}", ptPin));
            try {
                response = HttpRequest.get("https://plogin.m.jd.com/cgi-bin/ml/islogin")
                        .cookie(cookie)
                        .execute();
                respJo = JSONObject.parseObject(response.body());
                log.debug(StrUtil.format("[检查Cookie], pt_pin: {}, 接口2响应: {}", ptPin, respJo));
                if (CollUtil.isNotEmpty(respJo)) {
                    String islogin = respJo.getString("islogin");
                    if (StrUtil.equals("1", islogin)) {
                        //
                    } else if (StrUtil.equals("0", islogin)) {
                        expired = true;
                    } else {
                        remark = "planB京东接口返回未知状态";
                    }
                } else {
                    remark = "planB京东接口返回空数据";
                }
            } catch (Exception e) {
                // ignore
                log.debug(StrUtil.format("[检查Cookie], pt_pin: {}, 接口2响应异常, e: {}", ptPin, e.getMessage()));
            }
        }

        CheckCookieResult result = new CheckCookieResult().setExpired(expired).setPtPin(ptPin).setNickName(nickName).setRemark(remark);
        log.debug(StrUtil.format("[检查Cookie], pt_pin: {}, 最终结果: {}", ptPin, JSONObject.toJSONString(result)));
        return result;
    }

    public static CfdExchangeResult exchangeCfd(String cookie) {
        HttpResponse response = HttpRequest.get(getCfdUrl(cookie))
                .cookie(cookie)
                .header(Header.REFERER, "https://st.jingxi.com/")
                .header(Header.USER_AGENT, "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1")
                .execute();

        log.debug(StrUtil.format("[兑换财富岛红包] 响应: {}", response.body()));
        JSONObject responseJo = JSON.parseObject(response.body());
        Integer iRet = responseJo.getInteger("iRet");
        String result;
        if (iRet == 0) {
            result = "抢到了";
        } else {
            result = responseJo.getString("sErrMsg");
        }
        log.debug(StrUtil.format("[兑换财富岛红包] 最终结果: {}", result));
        return new CfdExchangeResult().setPtPin(JDXUtil.getPtPinFromCK(cookie)).setResult(result);
    }

    public static String getCfdUrl(String cookie) {
        try {
            HttpResponse response = HttpRequest.get("https://m.jingxi.com/jxbfd/user/ExchangeState?strZone=jxbfd&dwType=2&sceneval=2&g_login_type=1")
                    .cookie(cookie)
                    .header(Header.REFERER, "https://st.jingxi.com/")
                    .header(Header.USER_AGENT, "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1")
                    .execute();
            log.debug(StrUtil.format("[获取财富岛100URL] 响应: {}", response.body()));
            JSONObject responseJo = JSON.parseObject(response.body());
            JSONObject hb100 = responseJo.getJSONArray("hongbao").stream()
                    .map(e -> JSON.parseObject(JSON.toJSONString(e)))
                    .filter(e -> e.getInteger("ddwPrice") == 10000)
                    .findFirst().orElse(new JSONObject());
            String hongbaoPool = responseJo.getString("hongbaopool");
            String url = StrUtil.format("https://m.jingxi.com/jxbfd/user/ExchangePrize?strZone=jxbfd&dwType=3&dwLvl={}&ddwPaperMoney=100000&strPoolName={}&sceneval=2&g_login_type=1", hb100.getString("dwLvl"), hongbaoPool);
            log.debug(StrUtil.format("[获取财富岛100URL] 最终返回结果: {}", url));
            return url;
        } catch (Exception e) {
            return "";
        }
    }

    @Data
    @Accessors(chain = true)
    public static class CheckCookieResult {

        private String id;
        private String ptPin;
        private String nickName;
        private boolean expired;
        private String remark;
    }

    @Data
    @Accessors(chain = true)
    public static class CfdExchangeResult {

        private String cookie;
        private String ptPin;
        private String result;
    }
}
