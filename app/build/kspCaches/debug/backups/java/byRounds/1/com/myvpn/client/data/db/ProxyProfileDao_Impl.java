package com.myvpn.client.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.myvpn.client.data.model.ProxyProfile;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ProxyProfileDao_Impl implements ProxyProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ProxyProfile> __insertionAdapterOfProxyProfile;

  private final EntityDeletionOrUpdateAdapter<ProxyProfile> __deletionAdapterOfProxyProfile;

  private final EntityDeletionOrUpdateAdapter<ProxyProfile> __updateAdapterOfProxyProfile;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastConnected;

  public ProxyProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfProxyProfile = new EntityInsertionAdapter<ProxyProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `proxy_profiles` (`id`,`name`,`serverAddress`,`serverPort`,`username`,`password`,`isDefault`,`createdAt`,`lastConnectedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProxyProfile entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getServerAddress());
        statement.bindLong(4, entity.getServerPort());
        statement.bindString(5, entity.getUsername());
        statement.bindString(6, entity.getPassword());
        final int _tmp = entity.isDefault() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getCreatedAt());
        if (entity.getLastConnectedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getLastConnectedAt());
        }
      }
    };
    this.__deletionAdapterOfProxyProfile = new EntityDeletionOrUpdateAdapter<ProxyProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `proxy_profiles` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProxyProfile entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfProxyProfile = new EntityDeletionOrUpdateAdapter<ProxyProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `proxy_profiles` SET `id` = ?,`name` = ?,`serverAddress` = ?,`serverPort` = ?,`username` = ?,`password` = ?,`isDefault` = ?,`createdAt` = ?,`lastConnectedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProxyProfile entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getServerAddress());
        statement.bindLong(4, entity.getServerPort());
        statement.bindString(5, entity.getUsername());
        statement.bindString(6, entity.getPassword());
        final int _tmp = entity.isDefault() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getCreatedAt());
        if (entity.getLastConnectedAt() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getLastConnectedAt());
        }
        statement.bindLong(10, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateLastConnected = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE proxy_profiles SET lastConnectedAt = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertProfile(final ProxyProfile profile,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfProxyProfile.insertAndReturnId(profile);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteProfile(final ProxyProfile profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfProxyProfile.handle(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateProfile(final ProxyProfile profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfProxyProfile.handle(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLastConnected(final long id, final long timestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLastConnected.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateLastConnected.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ProxyProfile>> getAllProfiles() {
    final String _sql = "SELECT * FROM proxy_profiles ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"proxy_profiles"}, new Callable<List<ProxyProfile>>() {
      @Override
      @NonNull
      public List<ProxyProfile> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfServerAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "serverAddress");
          final int _cursorIndexOfServerPort = CursorUtil.getColumnIndexOrThrow(_cursor, "serverPort");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastConnectedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastConnectedAt");
          final List<ProxyProfile> _result = new ArrayList<ProxyProfile>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ProxyProfile _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpServerAddress;
            _tmpServerAddress = _cursor.getString(_cursorIndexOfServerAddress);
            final int _tmpServerPort;
            _tmpServerPort = _cursor.getInt(_cursorIndexOfServerPort);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpPassword;
            _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpLastConnectedAt;
            if (_cursor.isNull(_cursorIndexOfLastConnectedAt)) {
              _tmpLastConnectedAt = null;
            } else {
              _tmpLastConnectedAt = _cursor.getLong(_cursorIndexOfLastConnectedAt);
            }
            _item = new ProxyProfile(_tmpId,_tmpName,_tmpServerAddress,_tmpServerPort,_tmpUsername,_tmpPassword,_tmpIsDefault,_tmpCreatedAt,_tmpLastConnectedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getProfileById(final long id,
      final Continuation<? super ProxyProfile> $completion) {
    final String _sql = "SELECT * FROM proxy_profiles WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ProxyProfile>() {
      @Override
      @Nullable
      public ProxyProfile call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfServerAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "serverAddress");
          final int _cursorIndexOfServerPort = CursorUtil.getColumnIndexOrThrow(_cursor, "serverPort");
          final int _cursorIndexOfUsername = CursorUtil.getColumnIndexOrThrow(_cursor, "username");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastConnectedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastConnectedAt");
          final ProxyProfile _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpServerAddress;
            _tmpServerAddress = _cursor.getString(_cursorIndexOfServerAddress);
            final int _tmpServerPort;
            _tmpServerPort = _cursor.getInt(_cursorIndexOfServerPort);
            final String _tmpUsername;
            _tmpUsername = _cursor.getString(_cursorIndexOfUsername);
            final String _tmpPassword;
            _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            final boolean _tmpIsDefault;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsDefault);
            _tmpIsDefault = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpLastConnectedAt;
            if (_cursor.isNull(_cursorIndexOfLastConnectedAt)) {
              _tmpLastConnectedAt = null;
            } else {
              _tmpLastConnectedAt = _cursor.getLong(_cursorIndexOfLastConnectedAt);
            }
            _result = new ProxyProfile(_tmpId,_tmpName,_tmpServerAddress,_tmpServerPort,_tmpUsername,_tmpPassword,_tmpIsDefault,_tmpCreatedAt,_tmpLastConnectedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
