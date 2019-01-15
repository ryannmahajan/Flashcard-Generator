package com.ryannm.android.ankimate;

import android.content.Context;

import com.ryannm.android.ankimate.Dao.BlackList;
import com.ryannm.android.ankimate.Dao.BlackListDao;
import com.ryannm.android.ankimate.Dao.DaoMaster;
import com.ryannm.android.ankimate.Dao.DaoSession;

import java.util.List;

public class BlackListLab {

    private Context mContext;
    private static BlackListLab sBlackListLab;
    private BlackListDao mBlackListDao;

    private BlackListLab(Context context) {
        mContext = context.getApplicationContext();
        updateSession();
    }

    public void updateSession() {
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext, "blacklists-db", null);
        DaoSession session = new DaoMaster(helper.getWritableDatabase()).newSession();
        mBlackListDao = session.getBlackListDao();
    }

    public static BlackListLab get(Context context) {
        if (sBlackListLab==null) {
            sBlackListLab = new BlackListLab(context);
        }
        return sBlackListLab;
    }

    public List<BlackList> getBlackLists() {
        return mBlackListDao.loadAll();
    }

    public BlackList getBlacklistById (String guid) {
        return mBlackListDao.load(guid);
    }

    public long insertOrReplaceBlacklist(BlackList blackList) {
        return mBlackListDao.insertOrReplace(blackList);
    }

    public void updateBlacklist(BlackList blacklist) {
        mBlackListDao.update(blacklist);
    }



}
