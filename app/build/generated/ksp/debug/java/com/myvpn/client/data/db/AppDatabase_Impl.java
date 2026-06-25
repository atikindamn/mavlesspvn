package com.myvpn.client.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile VpnProfileDao _vpnProfileDao;

  private volatile ProxyProfileDao _proxyProfileDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `vpn_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `serverAddress` TEXT NOT NULL, `serverPort` INTEGER NOT NULL, `uuid` TEXT NOT NULL, `flow` TEXT NOT NULL, `security` TEXT NOT NULL, `sni` TEXT NOT NULL, `publicKey` TEXT NOT NULL, `shortId` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, `network` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `lastConnectedAt` INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `proxy_profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `serverAddress` TEXT NOT NULL, `serverPort` INTEGER NOT NULL, `username` TEXT NOT NULL, `password` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `lastConnectedAt` INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f882769a9d422b88a6390d795381faaa')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `vpn_profiles`");
        db.execSQL("DROP TABLE IF EXISTS `proxy_profiles`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsVpnProfiles = new HashMap<String, TableInfo.Column>(15);
        _columnsVpnProfiles.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("serverAddress", new TableInfo.Column("serverAddress", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("serverPort", new TableInfo.Column("serverPort", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("uuid", new TableInfo.Column("uuid", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("flow", new TableInfo.Column("flow", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("security", new TableInfo.Column("security", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("sni", new TableInfo.Column("sni", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("publicKey", new TableInfo.Column("publicKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("shortId", new TableInfo.Column("shortId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("fingerprint", new TableInfo.Column("fingerprint", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("network", new TableInfo.Column("network", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("isDefault", new TableInfo.Column("isDefault", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsVpnProfiles.put("lastConnectedAt", new TableInfo.Column("lastConnectedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysVpnProfiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesVpnProfiles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoVpnProfiles = new TableInfo("vpn_profiles", _columnsVpnProfiles, _foreignKeysVpnProfiles, _indicesVpnProfiles);
        final TableInfo _existingVpnProfiles = TableInfo.read(db, "vpn_profiles");
        if (!_infoVpnProfiles.equals(_existingVpnProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "vpn_profiles(com.myvpn.client.data.model.VpnProfile).\n"
                  + " Expected:\n" + _infoVpnProfiles + "\n"
                  + " Found:\n" + _existingVpnProfiles);
        }
        final HashMap<String, TableInfo.Column> _columnsProxyProfiles = new HashMap<String, TableInfo.Column>(9);
        _columnsProxyProfiles.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProxyProfiles.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProxyProfiles.put("serverAddress", new TableInfo.Column("serverAddress", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProxyProfiles.put("serverPort", new TableInfo.Column("serverPort", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProxyProfiles.put("username", new TableInfo.Column("username", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProxyProfiles.put("password", new TableInfo.Column("password", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProxyProfiles.put("isDefault", new TableInfo.Column("isDefault", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProxyProfiles.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProxyProfiles.put("lastConnectedAt", new TableInfo.Column("lastConnectedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProxyProfiles = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProxyProfiles = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProxyProfiles = new TableInfo("proxy_profiles", _columnsProxyProfiles, _foreignKeysProxyProfiles, _indicesProxyProfiles);
        final TableInfo _existingProxyProfiles = TableInfo.read(db, "proxy_profiles");
        if (!_infoProxyProfiles.equals(_existingProxyProfiles)) {
          return new RoomOpenHelper.ValidationResult(false, "proxy_profiles(com.myvpn.client.data.model.ProxyProfile).\n"
                  + " Expected:\n" + _infoProxyProfiles + "\n"
                  + " Found:\n" + _existingProxyProfiles);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f882769a9d422b88a6390d795381faaa", "a3ffc3243bb8ca63d46718599bd32396");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "vpn_profiles","proxy_profiles");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `vpn_profiles`");
      _db.execSQL("DELETE FROM `proxy_profiles`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(VpnProfileDao.class, VpnProfileDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProxyProfileDao.class, ProxyProfileDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public VpnProfileDao vpnProfileDao() {
    if (_vpnProfileDao != null) {
      return _vpnProfileDao;
    } else {
      synchronized(this) {
        if(_vpnProfileDao == null) {
          _vpnProfileDao = new VpnProfileDao_Impl(this);
        }
        return _vpnProfileDao;
      }
    }
  }

  @Override
  public ProxyProfileDao proxyProfileDao() {
    if (_proxyProfileDao != null) {
      return _proxyProfileDao;
    } else {
      synchronized(this) {
        if(_proxyProfileDao == null) {
          _proxyProfileDao = new ProxyProfileDao_Impl(this);
        }
        return _proxyProfileDao;
      }
    }
  }
}
