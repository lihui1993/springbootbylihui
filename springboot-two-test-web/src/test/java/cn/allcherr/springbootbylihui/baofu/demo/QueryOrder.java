/**
 * Company: www.baofu.com
 * @author dasheng(大圣)
 * @date 2018年3月14日
 */
package cn.allcherr.springbootbylihui.baofu.demo;

import cn.allcheer.springbootbylihui.springboottwotestweb.SpringbootTwoTestWebApplication;
import cn.allcheer.springbootbylihui.utils.baofu.rsa.SignatureUtils;
import cn.allcheer.springbootbylihui.utils.baofu.util.FormatUtil;
import cn.allcheer.springbootbylihui.utils.baofu.util.HttpUtil;
import cn.allcheer.springbootbylihui.utils.baofu.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringbootTwoTestWebApplication.class)
@Slf4j
public class QueryOrder{
	/**
	 * 支付订单查询。
	 * @throws Exception
	 */
	public static void main() throws Exception {
		String send_time=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());//报文发送日期时间		
		String  pfxpath ="D:\\CER_EN_DECODE\\AgreementPay\\bfkey_100025773@@200001173.pfx";//商户私钥        
        String  cerpath = "D:\\CER_EN_DECODE\\AgreementPay\\bfkey_100025773@@200001173.cer";//宝付公钥


		Map<String,String> DateArry = new TreeMap<String,String>();
		DateArry.put("send_time", send_time);
		DateArry.put("msg_id", "TISN"+System.currentTimeMillis());//报文流水号
		DateArry.put("version", "4.0.0.0");
		DateArry.put("terminal_id", "200001173");
		DateArry.put("txn_type", "07");//交易类型
		DateArry.put("member_id", "100025773");
		DateArry.put("orig_trans_id", "TID1520991457756");//交易时的trans_id
		DateArry.put("orig_trade_date", send_time);//
		
		String SignVStr = FormatUtil.coverMap2String(DateArry);
		log.info("SHA-1摘要字串："+SignVStr);
		String signature = SecurityUtil.sha1X16(SignVStr, "UTF-8");//签名
		log.info("SHA-1摘要结果："+signature);
		String Sign = SignatureUtils.encryptByRSA(signature, pfxpath, "100025773_286941");
		log.info("RSA签名结果："+Sign);
		DateArry.put("signature", Sign);//签名域
		
		String PostString  = HttpUtil.RequestForm("https://vgw.baofu.com/cutpayment/protocol/backTransRequest", DateArry);
		log.info("请求返回:"+PostString);
		
		Map<String, String> ReturnData = FormatUtil.getParm(PostString);
		
		if(!ReturnData.containsKey("signature")){
			throw new Exception("缺少验签参数！");
		}
		
		String RSign=ReturnData.get("signature");
		log.info("返回的验签值："+RSign);
		ReturnData.remove("signature");//需要删除签名字段		
		String RSignVStr = FormatUtil.coverMap2String(ReturnData);
		log.info("返回SHA-1摘要字串："+RSignVStr);
		String RSignature = SecurityUtil.sha1X16(RSignVStr, "UTF-8");//签名
		log.info("返回SHA-1摘要结果："+RSignature);
		
		if(SignatureUtils.verifySignature(cerpath,RSignature,RSign)){
			log.info("Yes");//验签成功
		}
		if(!ReturnData.containsKey("resp_code")){
			throw new Exception("缺少resp_code参数！");
		}
		if(ReturnData.get("resp_code").toString().equals("S")){	
			log.info("交易成功！");
		}else if(ReturnData.get("resp_code").toString().equals("I")){	
			log.info("处理中！");
		}else if(ReturnData.get("resp_code").toString().equals("F")){	
			log.info("失败！");
		}else{
			throw new Exception("反回异常！");//异常不得做为订单状态。
		}
	}
}