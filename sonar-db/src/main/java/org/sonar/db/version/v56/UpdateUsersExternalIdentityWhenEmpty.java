/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.version.v56;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

/**
 * Update USERS.EXTERNAL_IDENTITY_PROVIDER to 'sonarqube' and USERS.EXTERNAL_IDENTITY to user's login when one of this 2 columns is null
 */
public class UpdateUsersExternalIdentityWhenEmpty extends BaseDataChange {

  private final System2 system2;

  public UpdateUsersExternalIdentityWhenEmpty(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate massUpdate = context.prepareMassUpdate();
    massUpdate.select("SELECT u.id, u.login FROM users u WHERE external_identity_provider IS NULL OR external_identity IS NULL");
    massUpdate.update("UPDATE users SET external_identity_provider=?, external_identity=?, updated_at=? WHERE id=?");
    massUpdate.rowPluralName("users");
    massUpdate.execute(new MigrationHandler(system2.now()));
  }

  private static class MigrationHandler implements MassUpdate.Handler {

    private final long now;

    public MigrationHandler(long now) {
      this.now = now;
    }

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      update.setString(1, "sonarqube");
      update.setString(2, row.getString(2));
      update.setLong(3, now);
      update.setLong(4, row.getLong(1));
      return true;
    }
  }

}
