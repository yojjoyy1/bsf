package com.weking.controller.pay;

import com.weking.cache.WKCache;
import com.weking.controller.out.OutControllerBase;
import com.weking.core.WkUtil;
import com.weking.core.gash.gash;
import com.weking.core.newebpay.H5NewebPay;
import com.weking.core.newebpay.NewebPay;
import com.weking.core.newebpay.NewebPayNew;
import com.weking.core.pay.PayssionUtil;
import com.weking.mapper.pocket.OrderMapper;
import com.weking.service.pay.PayService;
import com.wekingframework.core.LibProperties;
import com.wekingframework.core.LibSysUtils;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 供外回调及验证支付
 */
@Controller
@RequestMapping("/pay")
public class PayController extends OutControllerBase {
    private static Logger log = Logger.getLogger(PayController.class);
    @Resource
    private PayService payService;
    @Resource
    private OrderMapper orderMapper;

    /**
     * 苹果支付验证
     */
    @RequestMapping("/applePay")
    public void ApplePay(HttpServletRequest request, HttpServletResponse response) {
        String access_token = getParameter(request, "access_token");
        double api_version = LibSysUtils.toDouble(request.getParameter("api_version"),1.2);
        JSONObject object = WkUtil.checkToken(access_token);
        if (object.optInt("code") == 0) {
            int userId = object.optInt("user_id");
            String apple_receipt = getParameter(request, "apple_receipt");
            String order_sn = getParameter(request, "order_sn");
            object = payService.checkApplePay(userId, order_sn, apple_receipt);
        }
        out(response, object,api_version);
    }

    /**
     * 谷歌支付验证
     */
    @RequestMapping("/googlePay")
    public void GooglePay(HttpServletRequest request, HttpServletResponse response) {
        log.info("begin googlePay");
        String access_token = getParameter(request, "access_token");
        JSONObject object = WkUtil.checkToken(access_token);
        String signture = getParameter(request, "signture", "");
        Double api_version = getParameter(request,"api_version",0.0);
        String signtureData = getParameter(request, "signtureData", "");
        if(api_version > 3.0){
            try {
                signtureData = java.net.URLDecoder.decode(signtureData, "utf-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("signture:" + signture+"signtureData:" + signtureData);
        if (object.optInt("code") == 0) {
            int userId = object.optInt("user_id");
            String project_name = getParameter(request, "project_name", "");
            object = payService.checkGoogleSignture(userId, signture, signtureData, project_name);
        }
        out(response, object,api_version);
        log.info("end googlePay");
    }

    /**
     * 微信购买钻石回调验证
     */
    @SuppressWarnings("unchecked")
    @RequestMapping("/wxPay")
    public void WxPay(HttpServletRequest request, HttpServletResponse response) {
        String result;
        try {
            SortedMap<Object, Object> parameters = new TreeMap<>();
            InputStream inputStream = request.getInputStream();
            SAXReader reader = new SAXReader();
            Document document = reader.read(inputStream);// 读取输入流
            Element root = document.getRootElement();// 得到xml根元素
            List<Element> elementList = root.elements();// 得到根元素的所有子节点
            // 遍历所有子节点
            for (Element e : elementList)
                parameters.put(e.getName(), e.getText());
            // 释放资源
            inputStream.close();
            result = payService.checkWxPay(parameters);
        } catch (Exception e) {
            result = payService.checkWxPayError();
            e.printStackTrace();
        }
        out(response, result,1.2);
    }

    /**
     * 乐点gash购买钻石回调验证
     */
    @RequestMapping("/gashPay")
    public void gashPay(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 取得收到的資料
        BufferedReader br = new BufferedReader(new InputStreamReader(request.getInputStream()));
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        // 将资料解码
        String data = sb.toString();
        data = data.replace("data=", "");
        String result = "";
        if (!LibSysUtils.isNullOrEmpty(data)) {
            result = payService.checkGashPay(data);
        }
        //String data = request.getParameter("data");
        //log.info("gash_back:"+data);
        out(response, result,1.2);
    }

    /**
     * Gash支付请款
     */
    @RequestMapping("/gashSettle")
    public void gashSettle(HttpServletRequest request, HttpServletResponse response) {
        String orderSn = getParameter(request, "order_sn", "");
        String amount = getParameter(request, "amount", "");
        JSONObject object = new JSONObject();
        object.put("amount", amount);
        object.put("order_sn", orderSn);
        WKCache.add_gash_settle_info(orderSn, object.toString());
        // gash.settle(orderSn,amount);
    }

    /**
     * Gash查单
     */
    @RequestMapping("/gashOrder")
    public void gashOrder(HttpServletRequest request, HttpServletResponse response) {
        String orderSn = getParameter(request, "order_sn", "");
        String amount = getParameter(request, "amount", "");
        gash.checkOrder(orderSn, amount);
    }

    /**
     * Gash跳转H5页面
     */
    @RequestMapping("/gashJump")
    public void gashJump(HttpServletRequest request, HttpServletResponse response) {
        String data = getParameter(request, "data", "");
        JSONObject dataObj = gash.checkData(data);
        String msg;
        if (dataObj.getBoolean("is_success")) {
            payService.checkGashPay(data);
            msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.success");
        } else {
            int rcode = dataObj.optInt("rcode");
            switch (rcode) {
                case 3801:
                    msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.3801");
                    break;
                case 3802:
                    msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.3802");
                    break;
                case 3803:
                    msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.3803");
                    break;
                case 3901:
                    msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.3901");
                    break;
                case 3902:
                    msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.3902");
                    break;
                case 3903:
                    msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.3903");
                    break;
                case 3904:
                    msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.3904");
                    break;
                case 3905:
                    msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.3905");
                    break;
                case 3906:
                    msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.3906");
                    break;
                default:
                    msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.error");
                    break;
            }
        }
        request.setAttribute("msg", msg);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/success.jsp");
        try {
            dispatcher.forward(request, response);
        } catch (ServletException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 支付宝回调验证
     */
    @RequestMapping("/alipay")
    public void AliPay(HttpServletRequest request, HttpServletResponse response) {
        String result = getParameter(request, "");
        out(response, result,1.2);
    }

    /**
     * ipay88 购买钻石回调验证
     */
    @RequestMapping("/ipay88")
    public void ipay88(HttpServletRequest request, HttpServletResponse response ) throws IOException {

        log.error("ipay88 callback data : "+request.getParameter("MerchantCode"));

        String merchantCode = request.getParameter("MerchantCode");
        String paymentId = request.getParameter("PaymentId");
        String refNo = request.getParameter("RefNo");
        String amount = request.getParameter("Amount");
        String currency = request.getParameter("Currency");
        String remark = request.getParameter("Remark");
        String transId = request.getParameter("TransId");
        String authCode = request.getParameter("AuthCode");
        String status = request.getParameter("Status");
        String errDesc = request.getParameter("ErrDesc");
        String signature = request.getParameter("Signature");

        log.error(String.format("%s_%s_%s_%s_%s_%s_%s_%s_%s_%s_%s",merchantCode,paymentId,refNo,amount,currency,remark,
                transId,authCode,status,errDesc,signature));

        String result = payService.checkiPay88(merchantCode,paymentId,refNo,amount,currency,remark,transId,
                authCode,status,errDesc,signature);
        log.error("result:"+result);
        out(response, result,1.2);

    }


    /**
     * NewebPay 跳转H5页面
     */
    @RequestMapping("/NewebPayJump")
    public void NewebPayJump(HttpServletRequest request, HttpServletResponse response) {
        log.info("跳转h5:===============");
        String Status = getParameter(request, "Status", "");
        log.info("Status:==============="+Status);
        if (Status.equalsIgnoreCase("SUCCESS")) {
            String tradeInfo = getParameter(request, "TradeInfo", "");
            log.info("tradeInfo:" + tradeInfo);
            //解密
            String decrypt = NewebPay.decrypt(NewebPay.hexStringToByteArray(tradeInfo));
            log.info("解密之后-----decrypt:" + decrypt);
            JSONObject dataObj = JSONObject.fromObject(decrypt);
            log.info("解密转json-----dataObj:" + dataObj);
            String status = dataObj.optString("Status");
            String msg;
            if (status.equalsIgnoreCase("SUCCESS")) {
                //交易成功加钱
                msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.success");
                JSONObject result = dataObj.optJSONObject("Result");
                String merchantOrderNo = result.optString("MerchantOrderNo");//订单号
                String tradeNo = result.optString("TradeNo");
                int count = orderMapper.selectCountByTradeNo(tradeNo);
                if(count==0){
                    log.info("tradeNo-----tradeNo:" + tradeNo);
                    payService.updateMerchantOrderNo(merchantOrderNo, tradeNo);
                }else {
                    request.setAttribute("msg", msg);
                    RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_success.jsp");
                }
            } else {
                log.error("解密之后-----status:" + status);
                msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.error");
            }
            request.setAttribute("msg", msg);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_success.jsp");
            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * NewebPay 跳转H5页面
     */
    @RequestMapping("/H5NewebPayJump")
    public void H5NewebPayJump(HttpServletRequest request, HttpServletResponse response) {
        log.info("非官网加密文件跳转h5:===============");
        String Status = getParameter(request, "Status", "");
        log.info("非官网加密文件Status:==============="+Status);
        if (Status.equalsIgnoreCase("SUCCESS")) {
            String tradeInfo = getParameter(request, "TradeInfo", "");
            log.info("非官网加密文件tradeInfo:" + tradeInfo);
            //解密
            String decrypt = H5NewebPay.decrypt(H5NewebPay.hexStringToByteArray(tradeInfo));
            log.info("非官网加密文件解密之后-----decrypt:" + decrypt);
            JSONObject dataObj = JSONObject.fromObject(decrypt);
            log.info("非官网加密文件解密转json-----dataObj:" + dataObj);
            String status = dataObj.optString("Status");
            String msg;
            if (status.equalsIgnoreCase("SUCCESS")) {
                //交易成功加钱
                msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.success");
                JSONObject result = dataObj.optJSONObject("Result");
                String merchantOrderNo = result.optString("MerchantOrderNo");//订单号
                String tradeNo = result.optString("TradeNo");
                int count = orderMapper.selectCountByTradeNo(tradeNo);
                if(count==0){
                    log.info("tradeNo-----tradeNo:" + tradeNo);
                    payService.updateMerchantOrderNo(merchantOrderNo, tradeNo);
                }else {
                    request.setAttribute("msg", msg);
                    RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_success_live.jsp");
                }
            } else {
                log.error("非官网加密文件解密之后-----status:" + status);
                msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.error");
            }
            request.setAttribute("msg", msg);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_success_live.jsp");
            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            request.setAttribute("msg", "交易失敗");
            log.info("====="+Status);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_fail_live.jsp");
            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * payNow 跳转H5页面
     */
    @RequestMapping("/payNowJump")
    public void payNowJump(HttpServletRequest request, HttpServletResponse response) {
        String msg;
        String tranStatus = getParameter(request, "TranStatus");
        String payType= getParameter(request,"PayType");
        String TotalPrice= getParameter(request,"TotalPrice");
        String card_foreign=null;
        log.error("回调====="+tranStatus);
        if (tranStatus.equalsIgnoreCase("S")) {
                //交易成功加钱
            msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.success");
            String buysafeNo = getParameter(request, "BuysafeNo");
            log.error("tradeNo-----tradeNo:" + buysafeNo);
            if("01".equals(payType)){
                card_foreign = getParameter(request, "Card_Foreign");
            }

            String orderNo = getParameter(request, "OrderNo");
            log.error("orderNo-----orderNo:" + orderNo);
            int count = orderMapper.selectCountByTradeNo(buysafeNo);
                if(count==0){
                    payService.updatePayNowOrder(orderNo, buysafeNo,card_foreign,TotalPrice);
                }else {
                    request.setAttribute("msg", msg);
                    RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_success.jsp");
                }

            request.setAttribute("msg", msg);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_success.jsp");

            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            msg="充值失败~";
            String ErrDes = getParameter(request, "ErrDes");
            log.info("ErrDes:==="+ErrDes);
            try {
                if(ErrDes!=null&&ErrDes!="") {
                    msg = URLDecoder.decode(ErrDes, "UTF-8");
                }
                if ("05".equals(payType)){
                    String ibonno = getParameter(request, "IBONNO");
                    if(ibonno!=null&&ibonno!=""){
                        ibonno=URLDecoder.decode(ibonno, "UTF-8");
                    }
                    String DueDate=getParameter(request,"DueDate");
                    if(DueDate!=null&&DueDate!=""){
                        DueDate=URLDecoder.decode(DueDate, "UTF-8");
                    }
                    msg="Ibon繳費代碼 :"+ibonno+"\n"+"繳款期限 :"+DueDate;
                }
                if ("03".equals(payType)){
                    String ATMNo = getParameter(request, "ATMNo");
                    if(ATMNo!=null&&ATMNo!=""){
                        ATMNo=URLDecoder.decode(ATMNo, "UTF-8");
                    }
                    String dueDate=getParameter(request,"DueDate");
                    if(dueDate!=null&&dueDate!=""){
                        dueDate=URLDecoder.decode(dueDate, "UTF-8");
                    }
                    msg="虛擬帳號號碼 :"+ATMNo+"\n"+"繳款期限 :"+dueDate;
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            log.info("msg:==="+msg);
            request.setAttribute("msg", msg);

            RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_fail.jsp");
            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * yiPay 跳转H5页面
     */
    @RequestMapping("/yiPayJump")
    public void yiPayJump(HttpServletRequest request, HttpServletResponse response) {
        String msg="操作完成";
        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String transactionNo = getParameter(request, "transactionNo");
        String orderNo = getParameter(request, "orderNo");
        String type= getParameter(request,"type");
        String statusCode= getParameter(request,"statusCode");
        String amount= getParameter(request,"amount");
        log.error("回调====="+statusCode+"  yipay订单："+transactionNo+" orderNo:"+orderNo);
        if ("00".equals(statusCode)) {
            //交易成功加钱
            msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.success");


            log.error("orderNo-----orderNo:" + orderNo);
            int count = orderMapper.selectCountByTradeNo(transactionNo);
            if(count==0){
                payService.updateYiPayOrder(orderNo, transactionNo);
            }else {
                request.setAttribute("msg", msg);
                RequestDispatcher dispatcher = request.getRequestDispatcher("/yiPay_success.jsp");
            }

            request.setAttribute("msg", msg);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/yiPay_success.jsp");

            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            msg="支付失败";

                if ("3".equals(type)){
                    String pinCode = getParameter(request, "pinCode");
                    msg="超商繳費代碼 :"+pinCode+"\n";
                }
                if ("4".equals(type)){
                    String account = getParameter(request, "account");
                    String bankCode = getParameter(request, "bankCode");
                    String bankName = getParameter(request, "bankName");
                    String bankBranchCode = getParameter(request, "bankBranchCode");
                    String bankBranchName = getParameter(request, "bankBranchName");

                    msg="虛擬帳號帳⼾:"+account+"\n"+"繳款銀⾏代碼:"+bankCode+
                    "\n"+"繳款銀行名稱:"+bankName+"\n"+"繳款分⾏代碼:"+bankBranchCode
                    +"\n"+"繳款分⾏行行名稱:"+bankBranchName;
                }

            request.setAttribute("msg", msg);

            RequestDispatcher dispatcher = request.getRequestDispatcher("/yiPay_fail.jsp");
            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        out.print("ok");
        request.setAttribute("msg", msg);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/yiPay_success.jsp");
        try {
            dispatcher.forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }


}



    /**
     * NewebPay3 跳转H5页面
     */
    @RequestMapping("/NewebPayNewJump")
    public void NewebPayNewJump(HttpServletRequest request, HttpServletResponse response) {
        log.info("跳转h53:===============");
        String Status = getParameter(request, "Status", "");
        log.info("Status3:==============="+Status);
        if (Status.equalsIgnoreCase("SUCCESS")) {
            String tradeInfo = getParameter(request, "TradeInfo", "");
            log.info("tradeInfo3:" + tradeInfo);
            //解密
            String decrypt = NewebPayNew.decrypt(NewebPayNew.hexStringToByteArray(tradeInfo));
            JSONObject dataObj = JSONObject.fromObject(decrypt);
            log.info("解密转json-----dataObj3:" + dataObj);
            String status = dataObj.optString("Status");
            String msg;
            if (status.equalsIgnoreCase("SUCCESS")) {
                //交易成功加钱
                msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.success");
                JSONObject result = dataObj.optJSONObject("Result");
                String merchantOrderNo = result.optString("MerchantOrderNo");//订单号
                String tradeNo = result.optString("TradeNo");
                int count = orderMapper.selectCountByTradeNo(tradeNo);
                if(count==0){
                    payService.updateMerchantOrderNo(merchantOrderNo, tradeNo);
                }else {
                    request.setAttribute("msg", msg);
                    RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_success_new.jsp");
                }
            } else {
                log.error("解密之后3-----status:" + status);
                msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.error");
            }
            request.setAttribute("msg", msg);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_success_new.jsp");
            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            request.setAttribute("msg", "交易失敗");
            log.info("3====="+Status);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/newebpay_fail_new.jsp");
            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
            * 红阳 跳转H5页面
     */
    @RequestMapping("/hyPayJump")
    public void hyPayJump(HttpServletRequest request, HttpServletResponse response) {
        String msg="操作完成";
        PrintWriter out = null;
        try {
            out = response.getWriter();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String buysafeno = getParameter(request, "buysafeno");//红阳订单
        String orderNo = getParameter(request, "Td");//商家订单
        String Card_Type= getParameter(request,"Card_Type","");
        String Note2= getParameter(request,"Note2","");
        String errcode= getParameter(request,"errcode");
        String amount= getParameter(request,"amount");
        log.error("回调====="+errcode+"  hy订单："+buysafeno+" orderNo:"+orderNo+" Note2:"+Note2);
        if ("00".equals(errcode)) {
            //交易成功加钱
            msg = LibProperties.getLanguage("zh_CN", "weking.lang.app.gash.jump.success");


            int count = orderMapper.selectCountByTradeNo(buysafeno);
            if(count==0){
                payService.updatehyPayOrder(orderNo, buysafeno);
            }else {
                request.setAttribute("msg", msg);
                RequestDispatcher dispatcher = request.getRequestDispatcher("/hyPay_success.jsp");
            }

            request.setAttribute("msg", msg);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/hyPay_success.jsp");

            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            try {
            msg="支付失败~";
            //if ("ATM".equals(Note2)){//atm
                String EntityATM = getParameter(request, "EntityATM");
                String BankCode = getParameter(request, "BankCode");
                String BankName = getParameter(request, "BankName");
                if(!LibSysUtils.isNullOrEmpty(EntityATM)&&!LibSysUtils.isNullOrEmpty(BankCode)
                        &&!LibSysUtils.isNullOrEmpty(BankName)) {
                    EntityATM=URLDecoder.decode(EntityATM, "UTF-8");
                    BankCode=URLDecoder.decode(BankCode, "UTF-8");
                    BankName=URLDecoder.decode(BankName, "UTF-8");
                    msg = "ATM 轉帳帳號:" + EntityATM + "\n" + "ATM 轉帳銀行代碼:" + BankCode +
                            "\n" + "ATM 轉帳分行名稱:" + BankName;
                    //log.error("EntityATM====="+EntityATM+" BankCode："+BankCode+" BankName:"+BankName);
                }
            //}

            request.setAttribute("msg", msg);

            RequestDispatcher dispatcher = request.getRequestDispatcher("/hyPay_fail.jsp");

                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        out.print("ok");
        request.setAttribute("msg", msg);
        RequestDispatcher dispatcher = request.getRequestDispatcher("/hyPay_success.jsp");
        try {
            dispatcher.forward(request, response);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    /**
     * payssion 跳转H5页面
     */
    @RequestMapping("/payssionCallback")
    public void payssionCallback(HttpServletRequest request, HttpServletResponse response) {
        String msg = "";
        PrintWriter out = null;
        try {
        String state =request.getParameter("state");
        String orderNo =request.getParameter("order_id");
        String transactionNo =request.getParameter("transaction_id");
        log.info("payssionCallback==="+state+"==order_id:"+orderNo+"==transaction_id:"+transactionNo);

        out = response.getWriter();
        if ("completed".equals(state)) {
            //交易成功加钱
            msg = LibProperties.getLanguage("en_US", "weking.lang.app.gash.jump.success");
            int count = orderMapper.selectCountByTradeNo(transactionNo);
            if (count == 0) {
                payService.updatePayssion(orderNo, transactionNo);
            }
            request.setAttribute("msg", msg);
            RequestDispatcher dispatcher = request.getRequestDispatcher("/payssion_success.jsp");

            try {
                dispatcher.forward(request, response);
            } catch (ServletException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        } catch (IOException e) {
            e.printStackTrace();
        }

        out.print("200");


    }


    /**
     * payssion 跳转H5页面
     */
    @RequestMapping("/payssionReturnUrl")
    public void payssionReturnUrl(HttpServletRequest request, HttpServletResponse response) {
        String msg = "";
        PrintWriter out = null;
        try {
            String orderNo =request.getParameter("order_id");
            String transactionNo =request.getParameter("transaction_id");
            String state = PayssionUtil.getData(orderNo,transactionNo);
            log.info("payssionReturnUrl===state:"+state+"==order_id:"+orderNo+"==transaction_id:"+transactionNo);
            out = response.getWriter();
            if ("completed".equals(state)) {
                msg = LibProperties.getLanguage("en_US", "weking.lang.app.gash.jump.success");

                request.setAttribute("msg", msg);
                RequestDispatcher dispatcher = request.getRequestDispatcher("/payssion_success.jsp");

                try {
                    dispatcher.forward(request, response);
                } catch (ServletException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if ("cancelled".equals(state)){
                request.setAttribute("msg", "The deal was cancelled");
                RequestDispatcher dispatcher = request.getRequestDispatcher("/payssion_fail.jsp");

                try {
                    dispatcher.forward(request, response);
                } catch (ServletException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if ("error".equals(state)){
                request.setAttribute("msg", "Payment error");
                RequestDispatcher dispatcher = request.getRequestDispatcher("/payssion_fail.jsp");

                try {
                    dispatcher.forward(request, response);
                } catch (ServletException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                request.setAttribute("msg", "Payment Failure");
                RequestDispatcher dispatcher = request.getRequestDispatcher("/payssion_fail.jsp");
                try {
                    dispatcher.forward(request, response);
                } catch (ServletException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        out.print("200");


    }


}
