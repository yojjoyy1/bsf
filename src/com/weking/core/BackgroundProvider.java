package com.weking.core;

import com.weking.cache.WKCache;
import com.weking.core.gash.gash;
import com.weking.service.admin.AdminService;
import com.weking.service.digital.DigitalService;
import com.weking.service.game.GameService;
import com.weking.service.live.InviteService;
import com.weking.service.live.LiveGuardService;
import com.weking.service.live.LiveService;
import com.weking.service.live.VideoChatService;
import com.weking.service.pay.PayService;
import com.weking.service.post.PostService;
import com.weking.service.shop.OrderService;
import com.weking.service.system.RobotService;
import com.wekingframework.core.LibDateUtils;
import com.wekingframework.core.LibSysUtils;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Administrator on 2017/2/22.
 */
public class BackgroundProvider {
    static Logger log = Logger.getLogger(BackgroundProvider.class);
    private static volatile LiveService liveService;
    private static volatile PayService payService;
    private static volatile RobotService robotService;
    private static volatile OrderService orderService;
    private static volatile VideoChatService videoChatService;
    private static volatile GameService gameService;
    private static volatile InviteService inviteService;
    private static volatile LiveGuardService liveGuardService;
    private static volatile PostService postService;
    private static volatile DigitalService digitalService;
    private static volatile AdminService adminService;

    static {
        init();
    }

    private static void init() {
        if (liveService == null) {
            synchronized (BackgroundProvider.class) {
                if (liveService == null) {
                    //ApplicationContext ctx = new ClassPathXmlApplicationContext("config/spring-common.xml");
                    liveService = (LiveService) SpringContextUtil.getBean("liveService");
                }
            }
        }
        if (robotService == null) {
            synchronized (BackgroundProvider.class) {
                if (robotService == null) {
                    robotService = (RobotService) SpringContextUtil.getBean("robotService");
                }
            }
        }
        if (payService == null) {
            synchronized (BackgroundProvider.class) {
                if (payService == null) {
                    payService = (PayService) SpringContextUtil.getBean("payService");
                }
            }
        }
        if (orderService == null) {
            synchronized (BackgroundProvider.class) {
                if (orderService == null) {
                    orderService = (OrderService) SpringContextUtil.getBean("orderService");
                }
            }
        }

        if (videoChatService == null) {
            synchronized (BackgroundProvider.class) {
                if (videoChatService == null) {
                    videoChatService = (VideoChatService) SpringContextUtil.getBean("videoChatService");
                }
            }
        }

        if (gameService == null) {
            synchronized (BackgroundProvider.class) {
                if (gameService == null) {
                    gameService = (GameService) SpringContextUtil.getBean("gameService");
                }
            }
        }

        if (inviteService == null) {
            synchronized (BackgroundProvider.class) {
                if (inviteService == null) {
                    inviteService = (InviteService) SpringContextUtil.getBean("inviteService");
                }
            }
        }

        if (liveGuardService == null) {
            synchronized (BackgroundProvider.class) {
                if (liveGuardService == null) {
                    //ApplicationContext ctx = new ClassPathXmlApplicationContext("config/spring-common.xml");
                    liveGuardService = (LiveGuardService) SpringContextUtil.getBean("liveGuardService");
                }
            }
        }

        if (postService == null) {
            synchronized (BackgroundProvider.class) {
                if (postService == null) {
                    postService = (PostService) SpringContextUtil.getBean("postService");
                }
            }
        }
        if (digitalService == null) {
            synchronized (BackgroundProvider.class) {
                if (digitalService == null) {
                    digitalService = (DigitalService) SpringContextUtil.getBean("digitalService");
                }
            }
        }
        if (adminService == null) {
            synchronized (BackgroundProvider.class) {
                if (adminService == null) {
                    adminService = (AdminService) SpringContextUtil.getBean("adminService");
                }
            }
        }

    }

    /**
     * 房间心跳机制，判断主播是否已经非正常退出
     */
    public static void ServerCheckRoomHeart() throws ParseException {
        init();
      /*  //Thread current = Thread.currentThread();
        SimpleDateFormat dfs = new SimpleDateFormat("yyyyMMddHHmmss");
        //List<Map<String, String>> hash = WKCache.get_all_room();
        Map<String,Map<String,String>> allRoomMap = WKCache.get_all_room_map();
        Set<Map.Entry<String,Map<String,String>>> entry = allRoomMap.entrySet();
        for (Map.Entry<String,Map<String,String>> entry1 : entry){
            //log.info("ServerCheckRoomHeart:"+entry1.getKey()+"---"+entry1.getValue());
            Map<String, String> room_info = entry1.getValue();
            if (room_info != null) {
                int live_id = LibSysUtils.toInt(room_info.get("live_id"));
                if (live_id != 0) {
                    String slive_id = LibSysUtils.toString(live_id);
                    //boolean flg = liveService.checkLiveIsEnd(live_id);
                    long heart_time = LibSysUtils.toLong(room_info.get("heart_time"));
                    if (heart_time == 0) {
                        WKCache.del_room(live_id);
                    } else {
                        java.util.Date start = dfs.parse(LibSysUtils.toString(heart_time));
                        java.util.Date end = dfs.parse(dfs.format(new Date()));
                        log.info(String.format("----------check_heart:Live_id=%s,room_members:%d,begin_time:%s,curr_time:%s", slive_id, LibSysUtils.toInt(room_info.get("real_audience")), start, end));
                        long between = (end.getTime() - start.getTime()) / 1000;//时间差
                        if (between > 180) {//时间差
                            int user_id = LibSysUtils.toInt(room_info.get("user_id"));
                            String account = room_info.get("account");
                            liveService.endLive(user_id, account, live_id, 2);
                        } else {
                            String live_stream_id = room_info.get("live_stream_id");
                            int live_type = LibSysUtils.toInt(room_info.get("live_type"));
                            boolean robot;
                            if (live_type == 0)
                                //robot = SystemConstant.enable_robot;//是否启用机器人
                                robot=LibSysUtils.toBoolean(WKCache.get_system_cache(C.WKSystemCacheField.weking_config_robot), false);
                            else
                                robot = LibSysUtils.toBoolean(WKCache.get_system_cache("weking.config.vvip.robot"));//VVIP是否启用机器人
                            if (robot)
                                robotService.in_room(live_id, live_stream_id, 0, "", "", "", 0);
                        }
                    }
                }else {
                    live_id = LibSysUtils.toInt(StringUtils.replace(entry1.getKey(),WKCache._room_tag,""));
                    //log.info("del_room---------"+live_id);
                    WKCache.del_room(live_id);
                }
            }
        }*/
//        System.out.println(String.format("begin check heart[Thread-%d]:%s", current.getId(), dfs.parse(dfs.format(new Date()))));
//        int size = hash.size();
//        if (size > 0) {
//            System.out.println(String.format("heart_rooms：%d", hash.size()));
//            for (int i = 0; i < size; i++) {
//
//            }
//        }
        checkGashOrder();
        settleGashOrder();
        cancelShopOrder();
        checkVideoChat();
        checkInviteAppoint();
        checkLiveGuard();
        calculateAward();
        updateUserState();
        //System.out.println(String.format("end check heart"));

    }

    private static void updateUserState(){
        Map<String,Integer> userStateMap = ResourceUtil.userStateMap;
        Set<Map.Entry<String,Integer>> entries = userStateMap.entrySet();
        for (Map.Entry<String,Integer> entry :entries){
            String[] arr = StringUtils.split(entry.getKey(),"_");
            ResourceUtil.userStateMap.put(entry.getKey(),WKCache.getUserState(LibSysUtils.toInt(arr[0]),arr[1]));
        }
    }

    /**
     * 计算动态奖励
     */
    public static void calculateAward() {
        postService.calculateAward();
    }

    /**
     * 重试需要gash支付订单
     */
    public static void checkGashOrder() {
        Map<String, String> gashOrderMap = WKCache.get_all_gash_order();
        if (gashOrderMap != null && gashOrderMap.size() > 0) {
            for (Map.Entry<String, String> entry : gashOrderMap.entrySet()) {
                JSONObject gashOrderObj = JSONObject.fromObject(entry.getValue());
                String data = gash.checkOrder(gashOrderObj.getString("order_sn"), gashOrderObj.getString("amount"));
                WKCache.del_gash_order_info(entry.getKey());
                payService.checkGashPay(data);
            }
        }
    }

    /**
     * 请款gash支付订单
     */
    public static void settleGashOrder() {
        Map<String, String> gashOrderMap = WKCache.get_all_gash_settle();
        if (gashOrderMap != null && gashOrderMap.size() > 0) {
            for (Map.Entry<String, String> entry : gashOrderMap.entrySet()) {
                JSONObject gashOrderObj = JSONObject.fromObject(entry.getValue());
                String data = gash.settle(entry.getKey(), gashOrderObj.getString("amount"));
                WKCache.del_gash_settle_info(entry.getKey());
                payService.settleGashPay(data);
            }
        }
    }

    /**
     * 定时取消商城订单
     */
    public static void cancelShopOrder() {
        Map<String, String> map = orderService.getNotPayOrderMap();
        if (map.size() > 0) {
            long curTime = LibSysUtils.toLong(WkUtil.futureTime(-1440));
            Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                JSONObject orderInfo = JSONObject.fromObject(entry.getValue());
                if (orderInfo.getLong("add_time") <= curTime) {
                    orderService.cancelOrder(orderInfo.getInt("user_id"), orderInfo.getString("order_id"));
                    it.remove();
                } else {
                    break;
                }
            }
        }
    }


    /**
     * 检测视频聊天异常
     */
    public static void checkVideoChat() {
        System.out.println("=================checkVideoChat===========");
        Map<String, String> map = WKCache.getVideoChatConsumeTimeMap();
        if (map != null && map.size() > 0) {
            Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
            int chatTime = LibSysUtils.toInt(WKCache.get_system_cache("video.chat.time"));
            long curTime = System.currentTimeMillis();
            while (it.hasNext()) {
                Map.Entry<String, String> entry = it.next();
                if (curTime - LibSysUtils.toLong(entry.getValue()) > (chatTime + 30) * 1000) {
                    String[] arr = entry.getKey().split("_");
                    log.info(String.format("endVideoChat:%s_%s", arr[0], arr[1]));
                    videoChatService.end(LibSysUtils.toInt(arr[0]), LibSysUtils.toInt(arr[1]), 2);
                }
            }
        }
    }


    /**
     * 房间心跳机制，判断主播是否已经非正常退出
     */
    public static void ServerCheckRoomHeartEx(String account) {
        init();
        List<Map<String, String>> hash = WKCache.get_all_room();
        int size = hash.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                Map<String, String> room_info = hash.get(i);
                if (room_info != null && account.equals(room_info.get("account"))) {
                    int live_id = LibSysUtils.toInt(room_info.get("live_id"));
                    int user_id = LibSysUtils.toInt(room_info.get("user_id"));
                    liveService.endLive(user_id, account, live_id, 2);
                    break;
                }
            }
        }
    }

    public static void checkInviteAppoint() {
        System.out.println("=================checkInviteAppoint===========");
        if (!ResourceUtil.NotConfirmAppointmentMap.isEmpty()) {
            Iterator<Map.Entry<Integer, Long>> it = ResourceUtil.NotConfirmAppointmentMap.entrySet().iterator();
            long curTime = LibDateUtils.getLibDateTime();
            while (it.hasNext()) {
                Map.Entry<Integer, Long> entry = it.next(); //24*60*60*1000
                if (LibDateUtils.getDateTimeTick(entry.getValue(), curTime) > 24 * 60 * 60 * 1000) {
                    log.info(String.format("endInviteAppoint:%s_%s", entry.getKey(), entry.getValue()));
                    JSONObject object = inviteService.updateState(0, entry.getKey(), 0, false, null);
                    if (object.getInt("code") == ResultCode.success) {
                        ResourceUtil.NotConfirmAppointmentMap.remove(entry.getKey());
                    }
                }
            }
        }
    }


    public static void putAllNotConfirmAppoint() {
        inviteService.putAllNotConfirmAppoint();
    }

    public static void checkLiveGuard() {
        liveGuardService.checkCancelLiveGuard();
    }


    // 推送领取直播间礼物消息
    public static void liveRoomGiftPush() {
        liveService.liveRoomGiftPush();
    }

    // 今日未发文推送
    public static void todayNoPostPush() {
        postService.todayNoPostPush();
    }


    public static void userDividendSCA() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (LibSysUtils.toBoolean(WKCache.get_system_cache(C.WKSystemCacheField.dividend_switch), false)) {
                    digitalService.userDividendSCA();
                }
            }
        }, "userDividendSCA");
        thread.start();
    }

    // 挖矿奖励
    public static void userMiningReward() {
        JSONObject post_mining_emo = JSONObject.fromObject(WKCache.get_system_cache(C.WKSystemCacheField.post_mining_emo));
        if (post_mining_emo.optBoolean("on_off",false)){
            log.info("-------- 挖矿奖励 --------");
            int post_num = post_mining_emo.optInt("post_num",0);
            int comment_num = post_mining_emo.optInt("comment_num",0);
            int like_num = post_mining_emo.optInt("like_num",0);
            int share_num = post_mining_emo.optInt("share_num",0);
            long date = DateUtils.getFrontDay(LibDateUtils.getLibDateTime("yyyyMMdd"),1);
            Set<String> users = WKCache.getAllPostOperateUsers(date);
            if (users.size() > 0){
                for (String user_id : users){
                    String content = WKCache.getUserTodayPostOperate(LibSysUtils.toInt(user_id),date);
                    if (!LibSysUtils.isNullOrEmpty(content)){
                        JSONObject jsonObject = JSONObject.fromObject(content);
                        int post = jsonObject.optInt("post",0);
                        if (post >= post_num){
                            digitalService.userMiningReward(user_id,"post");
                        }
                        int comment = jsonObject.optInt("comment",0);
                        if (comment >= comment_num){
                            digitalService.userMiningReward(user_id,"comment");
                        }
                        int like = jsonObject.optInt("like",0);
                        if (like >= like_num){
                            digitalService.userMiningReward(user_id,"like");
                        }
                        int share = jsonObject.optInt("share",0);
                        if (share >= share_num){
                            digitalService.userMiningReward(user_id,"share");
                        }
                    }
                    WKCache.delUserPostOperate(LibSysUtils.toInt(user_id),date);
                }
            }
        }
    }


    // 定时核销paynow
    public static void payNowCancel() {
        adminService.payNowCancel();
    }


}
