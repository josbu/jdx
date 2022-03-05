package cn.yiidii.jdx.model.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.context.ApplicationEvent;

/**
 * 管理员通知事件
 *
 * @author ed w
 * @since 1.0
 */
@Getter
@Setter
@Accessors(chain = true)
public class AdminNotifyEvent extends ApplicationEvent {

    private static final Object NULL_OBJECT = new Object();

    private String title;
    private String content;

    public AdminNotifyEvent(String title, String content) {
        super(NULL_OBJECT);
        this.title = title;
        this.content = content;
    }
}
