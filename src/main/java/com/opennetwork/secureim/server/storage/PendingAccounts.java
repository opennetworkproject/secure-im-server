package com.opennetwork.secureim.server.storage;

import com.opennetwork.secureim.server.auth.StoredVerificationCode;
import com.opennetwork.secureim.server.auth.StoredVerificationCode;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface PendingAccounts {

  @SqlUpdate("WITH upsert AS (UPDATE pending_accounts SET verification_code = :verification_code, timestamp = :timestamp WHERE number = :number RETURNING *) " +
             "INSERT INTO pending_accounts (number, verification_code, timestamp) SELECT :number, :verification_code, :timestamp WHERE NOT EXISTS (SELECT * FROM upsert)")
  void insert(@Bind("number") String number, @Bind("verification_code") String verificationCode, @Bind("timestamp") long timestamp);

  @Mapper(StoredVerificationCodeMapper.class)
  @SqlQuery("SELECT verification_code, timestamp FROM pending_accounts WHERE number = :number")
  StoredVerificationCode getCodeForNumber(@Bind("number") String number);

  @SqlUpdate("DELETE FROM pending_accounts WHERE number = :number")
  void remove(@Bind("number") String number);

  @SqlUpdate("VACUUM pending_accounts")
  public void vacuum();

  public static class StoredVerificationCodeMapper implements ResultSetMapper<StoredVerificationCode> {
    @Override
    public StoredVerificationCode map(int i, ResultSet resultSet, StatementContext statementContext)
        throws SQLException
    {
      return new StoredVerificationCode(resultSet.getString("verification_code"),
                                        resultSet.getLong("timestamp"));
    }
  }
}
