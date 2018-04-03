package joker.john.com.joker;

import android.app.Application;
import android.content.Context;

/**
 * Created by liweifeng on 16/03/2018.
 */

public class App extends Application {
    private Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public Context getContext() {
        return context;
    }
}
