package com.github.gnastnosaj.pandora.datasource;

import android.content.Context;

import com.github.gnastnosaj.boilerplate.ui.activity.BaseActivity;
import com.shizhefei.mvc.IAsyncDataSource;
import com.shizhefei.mvc.RequestHandle;
import com.shizhefei.mvc.ResponseSender;
import com.trello.rxlifecycle2.android.ActivityEvent;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by LuckyJayce on 2016/7/21.
 */
public abstract class RxDataSource<DATA> implements IAsyncDataSource<DATA> {
    private Context context;

    public RxDataSource(Context context) {
        this.context = context;
    }

    @Override
    public final RequestHandle refresh(final ResponseSender<DATA> sender) throws Exception {
        Observable<DATA> refresh = refresh();
        if (context instanceof BaseActivity) {
            refresh = refresh.compose(((BaseActivity) context).bindUntilEvent(ActivityEvent.DESTROY));
        }
        Disposable disposable = refresh.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> sender.sendData(data), throwable -> sender.sendError(new Exception(throwable)));

        return new RequestHandle() {
            @Override
            public void cancle() {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            }

            @Override
            public boolean isRunning() {
                return false;
            }
        };
    }

    @Override
    public final RequestHandle loadMore(ResponseSender<DATA> sender) throws Exception {
        Observable<DATA> loadMore = loadMore();
        if (context instanceof BaseActivity) {
            loadMore = loadMore.compose(((BaseActivity) context).bindUntilEvent(ActivityEvent.DESTROY));
        }
        Disposable disposable = loadMore.subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> sender.sendData(data), throwable -> sender.sendError(new Exception(throwable)));

        return new RequestHandle() {
            @Override
            public void cancle() {
                if (!disposable.isDisposed()) {
                    disposable.dispose();
                }
            }

            @Override
            public boolean isRunning() {
                return false;
            }
        };
    }

    public abstract Observable<DATA> refresh() throws Exception;

    public abstract Observable<DATA> loadMore() throws Exception;
}
