package com.example.peanut.aidialoge;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;
import com.iflytek.aiui.AIUIAgent;
import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIEvent;
import com.iflytek.aiui.AIUIListener;
import com.iflytek.aiui.AIUIMessage;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static String TAG = MainActivity.class.getSimpleName();

    //录音权限
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private Toast mToast;
    private EditText mNlpText;

    private AIUIAgent mAIUIAgent = null;

    private Timer timer;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                sayHi();
            }
        }
    };

    //交互状态
    private int mAIUIState = AIUIConstant.STATE_IDLE;

    @SuppressLint("ShowToast")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initLayout();
        initTimer();

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        requestPermission();
        if( checkAIUIAgent() ){
            String ttsStr = "Hi,跟我说话吧";
            byte[] ttsData = new byte[0];  //转为二进制数据
            try {
                ttsData = ttsStr.getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            StringBuffer params = new StringBuffer();  //构建合成参数
            params.append("vcn=xiaoyan");  //合成发音人
            params.append(",speed=50");  //合成速度
            params.append(",pitch=50");  //合成音调
            params.append(",volume=50");  //合成音量

            AIUIMessage startTts = new AIUIMessage(AIUIConstant.CMD_TTS,AIUIConstant.START, 0, params.toString(), ttsData);
            mAIUIAgent.sendMessage(startTts);
        }

    }

    private void sayHi(){
        if( checkAIUIAgent() ){

            Random random = new Random();

            int i = random.nextInt(10);
            String ttsStr = "你好呀";
            switch (i){
                case 0:
                    ttsStr="来和我说话吧";
                    break;

                case 1:
                    ttsStr="要不要听我讲个故事";
                    break;

                case 2:
                    ttsStr="今天天气真好，我们一起吹泡泡，好吗？";
                    break;

                case 3:
                    ttsStr="我们一起吹个气球吧，爸爸妈妈也来吗";
                    break;

                case 4:
                    ttsStr="我喜欢你经常玩的那个玩具";
                    break;

                case 5:
                    ttsStr="我想去卫生间";
                    break;

                default:
                    ttsStr="你好,在干什么呢";
                    break;
            }
            byte[] ttsData = new byte[0];  //转为二进制数据
            try {
                ttsData = ttsStr.getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            StringBuffer params = new StringBuffer();  //构建合成参数
            params.append("vcn=xiaoyan");  //合成发音人
            params.append(",speed=50");  //合成速度
            params.append(",pitch=50");  //合成音调
            params.append(",volume=50");  //合成音量

            AIUIMessage startTts = new AIUIMessage(AIUIConstant.CMD_TTS,AIUIConstant.START, 0, params.toString(), ttsData);
            mAIUIAgent.sendMessage(startTts);

            showTip(ttsStr);
        }
    }

    private void initTimer(){

        timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // (1) 使用handler发送消息
                Message message=new Message();
                message.what=0;
                mHandler.sendMessage(message);
            }
        },10000,10000);
    }

    private void destroyTimer(){
        if(timer!=null){
            timer.cancel();
        }
    }

    /**
     * 初始化Layout。
     */
    private void initLayout() {
        findViewById(R.id.nlp_start).setOnClickListener(this);

        mNlpText = (EditText)findViewById(R.id.nlp_text);
    }

    @Override
    public void onClick(View view) {
        if( !checkAIUIAgent() ){
            return;
        }

        destroyTimer();
        initTimer();

        switch (view.getId()) {
            // 开始语音理解
            case R.id.nlp_start:
                startVoiceNlp();
                break;
            default:
                break;
        }
        String ttsStr = mNlpText.getText().toString();
        ttsStr.replaceAll("\\s*", "");
        if(!ttsStr.equals("")){
            byte[] ttsData = new byte[0];  //转为二进制数据
            try {
                ttsData = ttsStr.getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            StringBuffer params = new StringBuffer();  //构建合成参数
            params.append("vcn=xiaoyan");  //合成发音人
            params.append(",speed=50");  //合成速度
            params.append(",pitch=50");  //合成音调
            params.append(",volume=50");  //合成音量

            AIUIMessage startTts = new AIUIMessage(AIUIConstant.CMD_TTS,AIUIConstant.START, 0, params.toString(), ttsData);
            mAIUIAgent.sendMessage(startTts);
        }
    }

    /**
     * 读取配置
     */
    private String getAIUIParams() {
        String params = "";

        AssetManager assetManager = getResources().getAssets();
        try {
            InputStream ins = assetManager.open( "cfg/aiui_phone.cfg" );
            byte[] buffer = new byte[ins.available()];

            ins.read(buffer);
            ins.close();

            params = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return params;
    }

    private boolean checkAIUIAgent(){
        if( null == mAIUIAgent ){
            Log.i( TAG, "create aiui agent" );

            //创建AIUIAgent
            mAIUIAgent = AIUIAgent.createAgent( this, getAIUIParams(), mAIUIListener );
        }

        if( null == mAIUIAgent ){
            final String strErrorTip = "创建 AIUI Agent 失败！";
            showTip( strErrorTip );
            this.mNlpText.setText( strErrorTip );
        }

        return null != mAIUIAgent;
    }

    //开始录音
    private void startVoiceNlp(){
        Log.i( TAG, "start voice nlp" );
        mNlpText.setText("");

        // 先发送唤醒消息，改变AIUI内部状态，只有唤醒状态才能接收语音输入
        // 默认为oneshot 模式，即一次唤醒后就进入休眠，如果语音唤醒后，需要进行文本语义，请将改段逻辑copy至startTextNlp()开头处
        if( AIUIConstant.STATE_WORKING != 	this.mAIUIState ){
            AIUIMessage wakeupMsg = new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null);
            mAIUIAgent.sendMessage(wakeupMsg);
        }

        // 打开AIUI内部录音机，开始录音
        String params = "sample_rate=16000,data_type=audio";
        AIUIMessage writeMsg = new AIUIMessage( AIUIConstant.CMD_START_RECORD, 0, 0, params, null );
        mAIUIAgent.sendMessage(writeMsg);
    }

    //AIUI事件监听器
    private AIUIListener mAIUIListener = new AIUIListener() {

        @Override
        public void onEvent(AIUIEvent event) {
            switch (event.eventType) {
                case AIUIConstant.EVENT_WAKEUP:
                    //唤醒事件
                    Log.i( TAG,  "on event: "+ event.eventType );
                    showTip( "进入识别状态" );
                    break;

                case AIUIConstant.EVENT_RESULT: {
                    //结果事件
                    Log.i( TAG,  "on event: "+ event.eventType );
                    try {
                        JSONObject bizParamJson = new JSONObject(event.info);
                        JSONObject data = bizParamJson.getJSONArray("data").getJSONObject(0);
                        JSONObject params = data.getJSONObject("params");
                        JSONObject content = data.getJSONArray("content").getJSONObject(0);

                        if (content.has("cnt_id")) {
                            String cnt_id = content.getString("cnt_id");
                            JSONObject cntJson = new JSONObject(new String(event.data.getByteArray(cnt_id), "utf-8"));

                            String sub = params.optString("sub");
                            JSONObject result = cntJson.optJSONObject("intent");
                            if ("nlp".equals(sub) && result.length() > 2) {
                                // 解析得到语义结果
                                String str = "";
                                //在线语义结果
                                if(result.optInt("rc") == 0){
                                    JSONObject answer = result.optJSONObject("answer");
                                    if(answer != null){
                                        str = answer.optString("text");
                                    }
                                }else{
                                    str = "rc4，无法识别";
                                }
                                if (!TextUtils.isEmpty(str)){
                                    mNlpText.append(str);
                                }
                            }

                            else if ("tts".equals(sub)) {
                                if (content.has("cnt_id")) {
                                    String sid = event.data.getString("sid");
                                    String cnt_id2 = content.getString("cnt_id");
                                    byte[] audio = event.data.getByteArray(cnt_id2); //合成音频数据

                                    int dts = content.getInt("dts");
                                    int frameId = content.getInt("frame_id");// 音频段id，取值：1,2,3,...

                                    int percent = event.data.getInt("percent"); //合成进度

                                    boolean isCancel = "1".equals(content.getString("cancel"));  //合成过程中是否被取消
                                }
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }

                    //mNlpText.append( " " );
                } break;

                case AIUIConstant.EVENT_ERROR: {
                    //错误事件
                    Log.i( TAG,  "on event: "+ event.eventType );
                    mNlpText.append( "\n" );
                    mNlpText.append( "错误: "+event.arg1+"\n"+event.info );
                } break;

                case AIUIConstant.EVENT_VAD: {
                    //vad事件
                    if (AIUIConstant.VAD_BOS == event.arg1) {
                        //找到语音前端点
                        showTip("找到vad_bos");
                    } else if (AIUIConstant.VAD_EOS == event.arg1) {
                        //找到语音后端点
                        showTip("找到vad_eos");
                    } else {
                        showTip("" + event.arg2);
                    }
                } break;

                case AIUIConstant.EVENT_START_RECORD: {
                    //开始录音事件
                    Log.i( TAG,  "on event: "+ event.eventType );
                    showTip("开始录音");
                } break;

                case AIUIConstant.EVENT_STOP_RECORD: {
                    //停止录音事件
                    Log.i( TAG,  "on event: "+ event.eventType );
                    showTip("停止录音");
                } break;

                case AIUIConstant.EVENT_STATE: {	// 状态事件
                    mAIUIState = event.arg1;

                    if (AIUIConstant.STATE_IDLE == mAIUIState) {
                        // 闲置状态，AIUI未开启
                        showTip("STATE_IDLE");
                    } else if (AIUIConstant.STATE_READY == mAIUIState) {
                        // AIUI已就绪，等待唤醒
                        showTip("STATE_READY");
                    } else if (AIUIConstant.STATE_WORKING == mAIUIState) {
                        // AIUI工作中，可进行交互
                        showTip("STATE_WORKING");
                    }
                } break;

                case AIUIConstant.EVENT_TTS: {
                    switch (event.arg1) {
                        case AIUIConstant.TTS_SPEAK_BEGIN:
                            //showTip("开始播放");
                            break;

                        case AIUIConstant.TTS_SPEAK_PROGRESS:
                            //showTip("播放进度为" + event.data.getInt("percent"));     // 播放进度
                            break;

                        case AIUIConstant.TTS_SPEAK_PAUSED:
                            //showTip("暂停播放");
                            break;

                        case AIUIConstant.TTS_SPEAK_RESUMED:
                            //showTip("恢复播放");
                            break;

                        case AIUIConstant.TTS_SPEAK_COMPLETED:
                            //showTip("播放完成");
                            break;

                        default:
                            break;
                    }
                } break;

                default:
                    break;
            }
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( null != this.mAIUIAgent ){
            AIUIMessage stopMsg = new AIUIMessage(AIUIConstant.CMD_STOP, 0, 0, null, null);
            mAIUIAgent.sendMessage( stopMsg );

            this.mAIUIAgent.destroy();
            this.mAIUIAgent = null;
        }

        timer.cancel();
    }

    private void showTip(final String str)
    {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }


    //申请录音权限
    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int i = ContextCompat.checkSelfPermission(this, permissions[0]);
            if (i != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 321);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 321) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PERMISSION_GRANTED) {
                    this.finish();
                }
            }
        }
    }
}
