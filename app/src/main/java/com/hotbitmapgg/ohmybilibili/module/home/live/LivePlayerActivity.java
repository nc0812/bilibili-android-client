package com.hotbitmapgg.ohmybilibili.module.home.live;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hotbitmapgg.ohmybilibili.R;
import com.hotbitmapgg.ohmybilibili.base.RxBaseActivity;
import com.hotbitmapgg.ohmybilibili.module.user.UserInfoDetailsActivity;
import com.hotbitmapgg.ohmybilibili.network.RetrofitHelper;
import com.hotbitmapgg.ohmybilibili.utils.ConstantUtils;
import com.hotbitmapgg.ohmybilibili.utils.LogUtil;
import com.hotbitmapgg.ohmybilibili.widget.CircleImageView;
import com.hotbitmapgg.ohmybilibili.widget.livelike.LoveLikeLayout;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by hcc on 16/8/4 21:18
 * 100332338@qq.com
 * <p/>
 * 直播播放界面
 */
public class LivePlayerActivity extends RxBaseActivity
{

    @Bind(R.id.video_view)
    SurfaceView videoView;

    @Bind(R.id.bili_anim)
    ImageView mAnimView;

    @Bind(R.id.right_play)
    ImageView mRightPlayBtn;

    @Bind(R.id.bottom_layout)
    RelativeLayout mBottomLayout;

    @Bind(R.id.bottom_play)
    ImageView mBottomPlayBtn;

    @Bind(R.id.bottom_fullscreen)
    ImageView mBottomFullscreen;

    @Bind(R.id.video_start_info)
    TextView mLoadTv;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.user_pic)
    CircleImageView mUserPic;

    @Bind(R.id.user_name)
    TextView mUserName;

    @Bind(R.id.live_num)
    TextView mLiveNum;

    @Bind(R.id.love_layout)
    LoveLikeLayout mLoveLikeLayout;

    @Bind(R.id.bottom_love)
    ImageView mlove;

    private IjkMediaPlayer ijkMediaPlayer;

    private SurfaceHolder holder;

    private int flag = 0;

    private boolean isPlay = false;

    private AnimationDrawable mAnimViewBackground;

    private int cid;

    private String title;

    private int online;

    private String face;

    private String name;

    private int mid;

    @Override
    public int getLayoutId()
    {

        return R.layout.activity_live_details;
    }

    @Override
    public void initViews(Bundle savedInstanceState)
    {

        Intent intent = getIntent();
        if (intent != null)
        {
            cid = intent.getIntExtra(ConstantUtils.EXTRA_CID, 0);
            title = intent.getStringExtra(ConstantUtils.EXTRA_TITLE);
            online = intent.getIntExtra(ConstantUtils.EXTRA_ONLINE, 0);
            face = intent.getStringExtra(ConstantUtils.EXTRA_FACE);
            name = intent.getStringExtra(ConstantUtils.EXTRA_NAME);
            mid = intent.getIntExtra(ConstantUtils.EXTRA_MID, 0);
        }


        initVideo();
        initUserInfo();
        startAnim();
    }

    /**
     * 设置用户信息
     */
    private void initUserInfo()
    {
        Glide.with(LivePlayerActivity.this)
                .load(face)
                .centerCrop()
                .dontAnimate()
                .placeholder(R.drawable.ico_user_default)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(mUserPic);


        mUserName.setText(name);
        mLiveNum.setText(String.valueOf(online));
    }

    private void startAnim()
    {

        mAnimViewBackground = (AnimationDrawable) mAnimView.getBackground();
        mAnimViewBackground.start();
    }

    private void stopAnim()
    {

        mAnimViewBackground.stop();
        mAnimView.setVisibility(View.GONE);
        mLoadTv.setVisibility(View.GONE);
    }

    @Override
    public void initToolBar()
    {

        mToolbar.setTitle(title);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {

        if (item.getItemId() == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    private void initVideo()
    {

        holder = videoView.getHolder();
        ijkMediaPlayer = new IjkMediaPlayer();
        getLiveUrl();
    }

    private void getLiveUrl()
    {

        RetrofitHelper.getLiveUrlApi()
                .getLiveUrl(cid)
                .compose(this.bindToLifecycle())
                .map(responseBody -> {

                    try
                    {
                        String str = responseBody.string();
                        String result = str.substring(str.lastIndexOf("[") + 1,
                                str.lastIndexOf("]") - 1);
                        return result;
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                        return null;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Func1<String,Observable<Long>>()
                {

                    @Override
                    public Observable<Long> call(String s)
                    {

                        playVideo(s);
                        return Observable.timer(2000, TimeUnit.MILLISECONDS);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {

                    stopAnim();
                    isPlay = true;
                    videoView.setVisibility(View.VISIBLE);
                    mRightPlayBtn.setImageResource(R.drawable.ic_tv_stop);
                    mBottomPlayBtn.setImageResource(R.drawable.ic_portrait_stop);
                }, throwable -> {

                    LogUtil.all("直播地址url获取失败" + throwable.getMessage());
                });
    }

    private void playVideo(String uri)
    {

        try
        {
            ijkMediaPlayer.setDataSource(this, Uri.parse(uri));
            ijkMediaPlayer.setDisplay(holder);
            holder.addCallback(new SurfaceHolder.Callback()
            {

                @Override
                public void surfaceCreated(SurfaceHolder holder)
                {

                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
                {

                    ijkMediaPlayer.setDisplay(holder);
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder)
                {

                }
            });
            ijkMediaPlayer.prepareAsync();
            ijkMediaPlayer.start();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        ijkMediaPlayer.setKeepInBackground(false);
    }

    private void startBottomShowAnim()
    {

        mBottomLayout.setVisibility(View.VISIBLE);
        mRightPlayBtn.setVisibility(View.VISIBLE);
    }

    private void startBottomHideAnim()
    {

        mBottomLayout.setVisibility(View.GONE);
        mRightPlayBtn.setVisibility(View.GONE);
    }


    public static void launch(Activity activity, int cid, String title, int online, String face, String name, int mid)
    {

        Intent mIntent = new Intent(activity, LivePlayerActivity.class);
        mIntent.putExtra(ConstantUtils.EXTRA_CID, cid);
        mIntent.putExtra(ConstantUtils.EXTRA_TITLE, title);
        mIntent.putExtra(ConstantUtils.EXTRA_ONLINE, online);
        mIntent.putExtra(ConstantUtils.EXTRA_FACE, face);
        mIntent.putExtra(ConstantUtils.EXTRA_NAME, name);
        mIntent.putExtra(ConstantUtils.EXTRA_MID, mid);
        activity.startActivity(mIntent);
    }


    @OnClick(R.id.right_play)
    void rightPlay()
    {

        ControlVideo();
    }

    @OnClick(R.id.bottom_play)
    void bottomPlay()
    {

        ControlVideo();
    }

    @OnClick(R.id.bottom_fullscreen)
    void fullScreen()
    {

    }


    @OnClick(R.id.video_view)
    void showBottomLayout()
    {

        if (flag == 0)
        {
            startBottomShowAnim();
            flag = 1;
        } else
        {
            startBottomHideAnim();
            flag = 0;
        }
    }

    @OnClick(R.id.user_pic)
    void startUserInfo()
    {

        UserInfoDetailsActivity.launch(LivePlayerActivity.this, name, mid, face);
        ControlVideo();
        mRightPlayBtn.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.bottom_love)
    void clickLove()
    {

        mLoveLikeLayout.addLove();
    }


    private void ControlVideo()
    {

        if (isPlay)
        {
            ijkMediaPlayer.pause();
            isPlay = false;
            mRightPlayBtn.setImageResource(R.drawable.ic_tv_play);
            mBottomPlayBtn.setImageResource(R.drawable.ic_portrait_play);
        } else
        {
            ijkMediaPlayer.start();
            isPlay = true;
            mRightPlayBtn.setImageResource(R.drawable.ic_tv_stop);
            mBottomPlayBtn.setImageResource(R.drawable.ic_portrait_stop);
        }
    }

    @Override
    protected void onDestroy()
    {

        super.onDestroy();
        ijkMediaPlayer.release();
    }
}
