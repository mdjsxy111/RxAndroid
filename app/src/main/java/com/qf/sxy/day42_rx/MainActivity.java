package com.qf.sxy.day42_rx;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.qf.sxy.day42_rx.cache.MyDiskCache;
import com.qf.sxy.day42_rx.cache.MyMemoryCache;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {


    private ImageView iv;
    private MyMemoryCache myMemoryCache;
    private MyDiskCache myDiskCache;
    private String path ="http://image77.360doc.com/DownloadImg/2014/08/1215/44232728_2.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取内存缓存对象
        myMemoryCache = MyMemoryCache.getMyMemeryCache();
        myDiskCache = MyDiskCache.getMyDiskCache(MainActivity.this);

        iv = (ImageView) findViewById(R.id.iv);
    }

    public void MyClick(View view) {
//        baseUseRx1();
//        baseUseRx2();
        //baseUseRx3();
//        useMapOperation();

//        flatMap();
//        mergeData();
        //   timerData();

        //intervalData();
       // schedulerTask();
        concatAndFirst();

    }

    //用concat和first操作符  做三级缓存
    public void concatAndFirst(){
        Observable<Bitmap> memory = Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                Bitmap bp = myMemoryCache.getBitmap(path);
                if(bp!=null){
                    subscriber.onNext(bp);
                    Log.e("AAA","===memory=onNext=>");
                }else {
                    subscriber.onCompleted();
                    Log.e("AAA","===memory=onCompleted=>");
                }
            }
        });
        Observable<Bitmap> disk = Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                Bitmap bp = myDiskCache.getBitmap(path);
                if(bp!=null){
                    subscriber.onNext(bp);
                    Log.e("AAA","===disk=onNext=>");
                }else {
                    subscriber.onCompleted();
                    Log.e("AAA","===disk=onCompleted=>");
                }
            }
        });
        Observable<Bitmap> netWork = Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {

                InputStream is = HttpUtils.getInputStream(path);
                Bitmap bp = BitmapFactory.decodeStream(is);

                myMemoryCache.put(path,bp);
                myDiskCache.putBitmap(path,bp);

                if(bp!=null){
                    subscriber.onNext(bp);
                    Log.e("AAA","===netWork=onNext=>");
                }else {
                    subscriber.onCompleted();
                    Log.e("AAA","===netWork=onCompleted=>");
                }
            }
        });


        //内存  磁盘  网络
    Observable.concat(memory,disk,netWork)
            .first()//依次执行
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Bitmap>() {
                @Override
                public void onCompleted() {
                    Log.e("AAA","===subscribe=onCompleted=>");
                }

                @Override
                public void onError(Throwable e) {
                    Log.e("AAA","===subscribe=onError=>");
                }

                @Override
                public void onNext(Bitmap bitmap) {
                    Log.e("AAA","===subscribe=onNext=>");
                    iv.setImageBitmap(bitmap);
                }
            });

    }

    public void schedulerTask(){
        Schedulers.newThread().createWorker()
                .schedulePeriodically(new Action0() {
                    @Override
                    public void call() {
                        InputStream is =  HttpUtils.getInputStream("https://www.baidu.com");
                        String str = HttpUtils.getDataByInputStream(is);
                        Log.e("AAA","===>"+str);
                    }
                    //1,初始化的时间  2,间隔时间  3,单位
                },1000,1000,TimeUnit.MILLISECONDS);
    }

    //线程调度
    public void threadScheduler() {

//        Observable.just(1, 2, 3)
//                .subscribeOn(Schedulers.io())//指明你的订阅SubScribe在io线程
//                .observeOn(AndroidSchedulers.mainThread())//subscribe的回调在主线程
//                .subscribe(num -> Log.e("AAA", "===>" + num));

        Observable.just(1, 2, 3, 4)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.newThread())
                .map(new Func1<Integer, String>() {
                    @Override
                    public String call(Integer integer) {
                        return integer + 8 + "";
                    }
                })
                .observeOn(Schedulers.io())
                .map(new Func1<String, Integer>() {
                    @Override
                    public Integer call(String s) {
                        return Integer.parseInt(s);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        Log.e("AAA","==>"+integer);
                    }
                });

    }


    //周期性操作   每隔一段时间 执行操作
    Observable obs;
    Subscriber s;

    public void intervalData() {
        obs = Observable.interval(2, TimeUnit.SECONDS);
        s = new Subscriber() {
            @Override
            public void onCompleted() {
                Log.e("AAA", "==onCompleted==>");
            }

            @Override
            public void onError(Throwable e) {
                Log.e("AAA", "==onError==>");
            }

            @Override
            public void onNext(Object o) {
                Log.e("AAA", "==onNext==>");
            }
        };
        obs.subscribe(s);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //移除观察者
        if (obs != null && s != null) {
            Subscription subScription = obs.unsafeSubscribe(s);
            subScription.unsubscribe();
            ;
        }
    }

    //指定2s之后进行执行
    public void timerData() {
        Observable.timer(2, TimeUnit.SECONDS)
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Long aLong) {
                        Log.e("AAA", "==哈哈哈==>" + aLong);
                    }
                });
    }

    //merge
    int count = 0;
    String result = "";

    public void mergeData() {
        Observable obs1 = Observable.just("许中");
        Observable obs2 = Observable.just("赵明阳");
        Observable.merge(obs1, obs2)
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        Log.e("AAA", "==onCompleted==>");
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String s) {
                        Log.e("AAA", "==onNext==>");
                        result += s;
                        count++;
                        if (count == 2) {
                            Log.e("AAA", "==onNext==>" + result);
                        }
                    }
                });

    }


    //flatMap
    public void flatMap() {
        //1,把传入的事件创建成一个Observable对象
        //2,不发送Observable2对象 而是激活Observable对象  Observable2,开始发送数据
        //首先创建一个Observable1进行发送事件,将这些事件 汇入到一个Observable2
        //接下来由Observable2 进行发送事件
        //将Observable1的事件进行平铺
        Observable.just(1, 2, 3, 4, 5, 6).flatMap(
                new Func1<Integer, Observable<String>>() {
                    @Override
                    public Observable<String> call(Integer integer) {

                        return Observable.just(integer + "a", integer + "b", +integer + "c");
                    }
                }).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                Log.e("AAA", "===>" + s);
            }
        });
//        Observable<Integer> Observable1 = Observable.just(1,2,3);
//        Observable<Integer>Observable2 = Observable1.flatMap(integer -> Observable.just(integer));
//        Observable2.subscribe(new Action1<Integer>() {
//            @Override
//            public void call(Integer integer) {
//                Log.e("AAA","===>"+s);
//            }
//        });
    }

    //使用map操作符
    public void useMapOperation() {
        //map:类型转换   将一个事件转换成另一个事件
//        Observable.just("许中,不怕下雨")
//                .map(new Func1<String, Integer>() {
//                    @Override
//                    public Integer call(String s) {
//                        Log.e("AAA","=转换第一次=>"+s);
//                        return 2016;
//                    }
//                })
//                .map(new Func1<Integer, String>() {
//                    @Override
//                    public String call(Integer integer) {
//                        Log.e("AAA","=转换第二次=>"+integer);
//                        return "赵明阳,怕雨";
//                    }
//                }).subscribe(new Action1<String>() {
//            @Override
//            public void call(String s) {
//                Log.e("AAA","=最终的值=>"+s);
//            }
//        });

        //Observable.just("AAA").map(s ->"2016").map(k->2016).subscribe(f ->findViewById(R.id.);->Log.e("AAA","=最终的值=>"+s));
        Observable.just(Environment.getExternalStorageDirectory() + "/DCIM/a3.jpg")
                .map(new Func1<String, Bitmap>() {
                    @Override
                    public Bitmap call(String s) {
                        return BitmapFactory.decodeFile(s);
                    }
                }).subscribe(new Action1<Bitmap>() {
            @Override
            public void call(Bitmap bitmap) {
                iv.setImageBitmap(bitmap);
            }
        });

    }

    public void baseUseRx3() {
        String[] strings = {"许中", "赵明阳", "朱晓攀"};
        Observable.from(strings).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String s) {
                Log.e("AAA", "=AAA=>" + s);
            }
        });

    }

    public void baseUseRx2() {

//        Observable.just("赵明阳").subscribe(new Action1<String>() {
//            @Override
//            public void call(String s) {
//                Log.e("AAA","=Call=>"+s);
//            }
//        });
        Observable.just("赵明阳,喝酒呢").subscribe(s -> Log.e("AAA", "=Call=>" + s));
    }

    //1,基本的使用方式
    public void baseUseRx1() {
        //创建一个被观察者对象
        Observable<String> observable = Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                subscriber.onNext("Hello 许中RX");
                subscriber.onCompleted();
                // subscriber.onError();
            }
        });
        //创建订阅者
        Subscriber<String> subscriber = new Subscriber<String>() {
            @Override
            public void onCompleted() {//完成
                Log.e("AAA", "=onCompleted=>");
            }

            @Override
            public void onError(Throwable e) {//错误
                Log.e("AAA", "=onError=>");
            }

            @Override
            public void onNext(String s) {//行订阅
                Log.e("AAA", "===>" + s);
            }
        };
        //被观察者  进行订阅
        observable.subscribe(subscriber);

    }
}
