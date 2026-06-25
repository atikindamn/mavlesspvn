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
import com.myvpn.client.data.model.VpnProfile;
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
public final class VpnProfileDao_Impl implements VpnProfileDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<VpnProfile> __insertionAdapterOfVpnProfile;

  private final EntityDeletionOrUpdateAdapter<VpnProfile> __deletionAdapterOfVpnProfile;

  private final EntityDeletionOrUpdateAdapter<VpnProfile> __updateAdapterOfVpnProfile;

  private final SharedSQLiteStatement __preparedStmtOfClearDefaultProfile;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLastConnected;

  public VpnProfileDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVpnProfile = new EntityInsertionAdapter<VpnProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `vpn_profiles` (`id`,`name`,`serverAddress`,`serverPort`,`uuid`,`flow`,`security`,`sni`,`publicKey`,`shortId`,`fingerprint`,`network`,`isDefault`,`createdAt`,`lastConnectedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VpnProfile entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getServerAddress());
        statement.bindLong(4, entity.getServerPort());
        statement.bindString(5, entity.getUuid());
        statement.bindString(6, entity.getFlow());
        statement.bindString(7, entity.getSecurity());
        statement.bindString(8, entity.getSni());
        statement.bindString(9, entity.getPublicKey());
        statement.bindString(10, entity.getShortId());
        statement.bindString(11, entity.getFingerprint());
        statement.bindString(12, entity.getNetwork());
        final int _tmp = entity.isDefault() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindLong(14, entity.getCreatedAt());
        if (entity.getLastConnectedAt() == null) {
          statement.bindNull(15);
        } else {
          statement.bindLong(15, entity.getLastConnectedAt());
        }
      }
    };
    this.__deletionAdapterOfVpnProfile = new EntityDeletionOrUpdateAdapter<VpnProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `vpn_profiles` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VpnProfile entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfVpnProfile = new EntityDeletionOrUpdateAdapter<VpnProfile>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `vpn_profiles` SET `id` = ?,`name` = ?,`serverAddress` = ?,`serverPort` = ?,`uuid` = ?,`flow` = ?,`security` = ?,`sni` = ?,`publicKey` = ?,`shortId` = ?,`fingerprint` = ?,`network` = ?,`isDefault` = ?,`createdAt` = ?,`lastConnectedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VpnProfile entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getServerAddress());
        statement.bindLong(4, entity.getServerPort());
        statement.bindString(5, entity.getUuid());
        statement.bindString(6, entity.getFlow());
        statement.bindString(7, entity.getSecurity());
        statement.bindString(8, entity.getSni());
        statement.bindString(9, entity.getPublicKey());
        statement.bindString(10, entity.getShortId());
        statement.bindString(11, entity.getFingerprint());
        statement.bindString(12, entity.getNetwork());
        final int _tmp = entity.isDefault() ? 1 : 0;
        statement.bindLong(13, _tmp);
        statement.bindLong(14, entity.getCreatedAt());
        if (entity.getLastConnectedAt() == null) {
          statement.bindNull(15);
        } else {
          statement.bindLong(15, entity.getLastConnectedAt());
        }
        statement.bindLong(16, entity.getId());
      }
    };
    this.__preparedStmtOfClearDefaultProfile = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE vpn_profiles SET isDefault = 0";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLastConnected = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE vpn_profiles SET lastConnectedAt = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertProfile(final VpnProfile profile,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfVpnProfile.insertAndReturnId(profile);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteProfile(final VpnProfile profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfVpnProfile.handle(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateProfile(final VpnProfile profile,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfVpnProfile.handle(profile);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearDefaultProfile(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearDefaultProfile.acquire();
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
          __preparedStmtOfClearDefaultProfile.release(_stmt);
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
  public Flow<List<VpnProfile>> getAllProfiles() {
    final String _sql = "SELECT * FROM vpn_profiles ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vpn_profiles"}, new Callable<List<VpnProfile>>() {
      @Override
      @NonNull
      public List<VpnProfile> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfServerAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "serverAddress");
          final int _cursorIndexOfServerPort = CursorUtil.getColumnIndexOrThrow(_cursor, "serverPort");
          final int _cursorIndexOfUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "uuid");
          final int _cursorIndexOfFlow = CursorUtil.getColumnIndexOrThrow(_cursor, "flow");
          final int _cursorIndexOfSecurity = CursorUtil.getColumnIndexOrThrow(_cursor, "security");
          final int _cursorIndexOfSni = CursorUtil.getColumnIndexOrThrow(_cursor, "sni");
          final int _cursorIndexOfPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKey");
          final int _cursorIndexOfShortId = CursorUtil.getColumnIndexOrThrow(_cursor, "shortId");
          final int _cursorIndexOfFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "fingerprint");
          final int _cursorIndexOfNetwork = CursorUtil.getColumnIndexOrThrow(_cursor, "network");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastConnectedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastConnectedAt");
          final List<VpnProfile> _result = new ArrayList<VpnProfile>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VpnProfile _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpServerAddress;
            _tmpServerAddress = _cursor.getString(_cursorIndexOfServerAddress);
            final int _tmpServerPort;
            _tmpServerPort = _cursor.getInt(_cursorIndexOfServerPort);
            final String _tmpUuid;
            _tmpUuid = _cursor.getString(_cursorIndexOfUuid);
            final String _tmpFlow;
            _tmpFlow = _cursor.getString(_cursorIndexOfFlow);
            final String _tmpSecurity;
            _tmpSecurity = _cursor.getString(_cursorIndexOfSecurity);
            final String _tmpSni;
            _tmpSni = _cursor.getString(_cursorIndexOfSni);
            final String _tmpPublicKey;
            _tmpPublicKey = _cursor.getString(_cursorIndexOfPublicKey);
            final String _tmpShortId;
            _tmpShortId = _cursor.getString(_cursorIndexOfShortId);
            final String _tmpFingerprint;
            _tmpFingerprint = _cursor.getString(_cursorIndexOfFingerprint);
            final String _tmpNetwork;
            _tmpNetwork = _cursor.getString(_cursorIndexOfNetwork);
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
            _item = new VpnProfile(_tmpId,_tmpName,_tmpServerAddress,_tmpServerPort,_tmpUuid,_tmpFlow,_tmpSecurity,_tmpSni,_tmpPublicKey,_tmpShortId,_tmpFingerprint,_tmpNetwork,_tmpIsDefault,_tmpCreatedAt,_tmpLastConnectedAt);
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
  public Object getProfileById(final long id, final Continuation<? super VpnProfile> $completion) {
    final String _sql = "SELECT * FROM vpn_profiles WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<VpnProfile>() {
      @Override
      @Nullable
      public VpnProfile call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfServerAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "serverAddress");
          final int _cursorIndexOfServerPort = CursorUtil.getColumnIndexOrThrow(_cursor, "serverPort");
          final int _cursorIndexOfUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "uuid");
          final int _cursorIndexOfFlow = CursorUtil.getColumnIndexOrThrow(_cursor, "flow");
          final int _cursorIndexOfSecurity = CursorUtil.getColumnIndexOrThrow(_cursor, "security");
          final int _cursorIndexOfSni = CursorUtil.getColumnIndexOrThrow(_cursor, "sni");
          final int _cursorIndexOfPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKey");
          final int _cursorIndexOfShortId = CursorUtil.getColumnIndexOrThrow(_cursor, "shortId");
          final int _cursorIndexOfFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "fingerprint");
          final int _cursorIndexOfNetwork = CursorUtil.getColumnIndexOrThrow(_cursor, "network");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastConnectedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastConnectedAt");
          final VpnProfile _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpServerAddress;
            _tmpServerAddress = _cursor.getString(_cursorIndexOfServerAddress);
            final int _tmpServerPort;
            _tmpServerPort = _cursor.getInt(_cursorIndexOfServerPort);
            final String _tmpUuid;
            _tmpUuid = _cursor.getString(_cursorIndexOfUuid);
            final String _tmpFlow;
            _tmpFlow = _cursor.getString(_cursorIndexOfFlow);
            final String _tmpSecurity;
            _tmpSecurity = _cursor.getString(_cursorIndexOfSecurity);
            final String _tmpSni;
            _tmpSni = _cursor.getString(_cursorIndexOfSni);
            final String _tmpPublicKey;
            _tmpPublicKey = _cursor.getString(_cursorIndexOfPublicKey);
            final String _tmpShortId;
            _tmpShortId = _cursor.getString(_cursorIndexOfShortId);
            final String _tmpFingerprint;
            _tmpFingerprint = _cursor.getString(_cursorIndexOfFingerprint);
            final String _tmpNetwork;
            _tmpNetwork = _cursor.getString(_cursorIndexOfNetwork);
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
            _result = new VpnProfile(_tmpId,_tmpName,_tmpServerAddress,_tmpServerPort,_tmpUuid,_tmpFlow,_tmpSecurity,_tmpSni,_tmpPublicKey,_tmpShortId,_tmpFingerprint,_tmpNetwork,_tmpIsDefault,_tmpCreatedAt,_tmpLastConnectedAt);
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

  @Override
  public Object getDefaultProfile(final Continuation<? super VpnProfile> $completion) {
    final String _sql = "SELECT * FROM vpn_profiles WHERE isDefault = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<VpnProfile>() {
      @Override
      @Nullable
      public VpnProfile call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfServerAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "serverAddress");
          final int _cursorIndexOfServerPort = CursorUtil.getColumnIndexOrThrow(_cursor, "serverPort");
          final int _cursorIndexOfUuid = CursorUtil.getColumnIndexOrThrow(_cursor, "uuid");
          final int _cursorIndexOfFlow = CursorUtil.getColumnIndexOrThrow(_cursor, "flow");
          final int _cursorIndexOfSecurity = CursorUtil.getColumnIndexOrThrow(_cursor, "security");
          final int _cursorIndexOfSni = CursorUtil.getColumnIndexOrThrow(_cursor, "sni");
          final int _cursorIndexOfPublicKey = CursorUtil.getColumnIndexOrThrow(_cursor, "publicKey");
          final int _cursorIndexOfShortId = CursorUtil.getColumnIndexOrThrow(_cursor, "shortId");
          final int _cursorIndexOfFingerprint = CursorUtil.getColumnIndexOrThrow(_cursor, "fingerprint");
          final int _cursorIndexOfNetwork = CursorUtil.getColumnIndexOrThrow(_cursor, "network");
          final int _cursorIndexOfIsDefault = CursorUtil.getColumnIndexOrThrow(_cursor, "isDefault");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfLastConnectedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "lastConnectedAt");
          final VpnProfile _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpServerAddress;
            _tmpServerAddress = _cursor.getString(_cursorIndexOfServerAddress);
            final int _tmpServerPort;
            _tmpServerPort = _cursor.getInt(_cursorIndexOfServerPort);
            final String _tmpUuid;
            _tmpUuid = _cursor.getString(_cursorIndexOfUuid);
            final String _tmpFlow;
            _tmpFlow = _cursor.getString(_cursorIndexOfFlow);
            final String _tmpSecurity;
            _tmpSecurity = _cursor.getString(_cursorIndexOfSecurity);
            final String _tmpSni;
            _tmpSni = _cursor.getString(_cursorIndexOfSni);
            final String _tmpPublicKey;
            _tmpPublicKey = _cursor.getString(_cursorIndexOfPublicKey);
            final String _tmpShortId;
            _tmpShortId = _cursor.getString(_cursorIndexOfShortId);
            final String _tmpFingerprint;
            _tmpFingerprint = _cursor.getString(_cursorIndexOfFingerprint);
            final String _tmpNetwork;
            _tmpNetwork = _cursor.getString(_cursorIndexOfNetwork);
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
            _result = new VpnProfile(_tmpId,_tmpName,_tmpServerAddress,_tmpServerPort,_tmpUuid,_tmpFlow,_tmpSecurity,_tmpSni,_tmpPublicKey,_tmpShortId,_tmpFingerprint,_tmpNetwork,_tmpIsDefault,_tmpCreatedAt,_tmpLastConnectedAt);
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
