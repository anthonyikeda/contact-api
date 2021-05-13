package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLType;
import java.sql.Types;

public class V3__LoadAddresses extends BaseJavaMigration {

    private final Logger log = LoggerFactory.getLogger(V3__LoadAddresses.class);

    @Override
    public void migrate(Context context) throws Exception {
        try (PreparedStatement pstmt = context.getConnection()
        .prepareStatement("insert into address(contact_id, street_1, street_2, city, state, country, postal_code) values(?, ?, ?, ?, ?, ?, ?)")) {

            BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/db/migration/addresses.csv")));
            String line;
            int counter = 0;

            while((line = reader.readLine()) != null) {
                if(counter == 0) {
                    counter++;
                } else {
                    counter++;
                    System.out.println(line);
                    String[] vals = line.split(",");

                    pstmt.setLong(1, Long.parseLong(vals[0]));
                    pstmt.setString(2, vals[1]);
                    if (!vals[2].equals(""))
                        pstmt.setString(3, vals[2]);
                    else
                        pstmt.setNull(3, Types.VARCHAR);
                    pstmt.setString(4, vals[3]);
                    pstmt.setString(5, vals[4]);
                    pstmt.setString(6, "United States");
                    pstmt.setString(7, vals[5]);
                    pstmt.addBatch();

                    if (counter%10 == 0) {
                        int[] inserted = pstmt.executeBatch();
                        log.info("Inserted multiple rows", inserted);
                    }
                }
            }

            int[] inserted = pstmt.executeBatch();
            if (inserted.length > 0)
                log.info("Inserted final {} rows", inserted);
        }
    }
}
