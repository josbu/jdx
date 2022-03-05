package cn.yiidii.jdx.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.yiidii.jdx.model.ex.BizException;
import com.alibaba.fastjson.JSONObject;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * WXPushUtil
 *
 * @author ed w
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class WXPushUtil {

    private static final String PUSH_URL = "http://wxpusher.zjiecode.com/api/send/message";
    private static final String DYNAMIC_QR__URL = "http://wxpusher.zjiecode.com/api/fun/create/qrcode";

    public void send(String appToken, List<String> uids, String title, String content, String contentType) {
        JSONObject reqParamJo = new JSONObject();
        reqParamJo.put("appToken", appToken);
        reqParamJo.put("content", content);
        reqParamJo.put("summary", title);
        reqParamJo.put("contentType", contentType);
        reqParamJo.put("uids", uids);
        log.debug(StrUtil.format("wxPusher发送消息, 参数: {}", reqParamJo.toJSONString()));
        HttpResponse resp = HttpRequest.post(PUSH_URL)
                .body(reqParamJo.toJSONString())
                .execute();
        log.debug(StrUtil.format("wxPusher发送消息, 响应: {}", resp.body()));
    }

    public String getDynamicQR(String appToken, JSONObject extJo) {
        JSONObject param = new JSONObject();
        param.put("appToken", appToken);
        param.put("extra", extJo.toJSONString());

        try {
            HttpResponse resp = HttpRequest.post(DYNAMIC_QR__URL).body(param.toJSONString()).execute();
            log.debug(StrUtil.format("wxPusher 动态二维码, 响应: {}", resp.body()));
            return JSONObject.parseObject(resp.body()).getJSONObject("data").getString("url");
        } catch (Exception e) {
            throw new BizException("获取wxPusher动态二维码异常，请联系系统管理员");
        }
    }

}
