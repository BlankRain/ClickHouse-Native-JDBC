package com.github.housepower.jdbc.metadata;

import com.github.housepower.jdbc.AbstractITest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;

public class ClickHouseDatabaseMetadataTest extends AbstractITest {
    @Before
    public void setUp() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connection) throws Exception {
                connection.createStatement().execute("CREATE DATABASE IF NOT EXISTS test");
            }
        });
    }


    @Test
    public void notNullMetaData() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connection) throws Exception {
                DatabaseMetaData metaData = connection.getMetaData();
                Assert.assertNotNull(metaData);
            }
        });
    }

    @Test
    public void testMetadata() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connection) throws Exception {
                connection.createStatement().executeQuery(
                        "DROP TABLE IF EXISTS test.testMetadata");
                connection.createStatement().executeQuery(
                        "CREATE TABLE test.testMetadata("
                                + "foo Nullable(UInt32), bar UInt64) ENGINE = TinyLog");
                ResultSet columns = connection.getMetaData().getColumns(
                        null, "test", "testMetaData", null);
                while (columns.next()) {
                    String colName = columns.getString("COLUMN_NAME");
                    if ("foo".equals(colName)) {
                        Assert.assertEquals(columns.getString("TYPE_NAME"), "UInt32");
                    } else if ("bar".equals(columns.getString("COLUMN_NAME"))) {
                        Assert.assertEquals(columns.getString("TYPE_NAME"), "UInt64");
                    } else {
                        throw new IllegalStateException(
                                "Unexpected column name " + colName);
                    }
                }
            }
        });
    }

    @Test
    public void testMetadataColumns() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connection) throws Exception {
                connection.createStatement().executeQuery(
                        "DROP TABLE IF EXISTS test.testMetadata");
                connection.createStatement().executeQuery(
                        "CREATE TABLE test.testMetadata("
                                + "foo Float32) ENGINE = TinyLog");
                ResultSet columns = connection.getMetaData().getColumns(
                        null, "test", "testMetadata", null);
                columns.next();
                Assert.assertEquals(columns.getString("TABLE_CAT"), "default");
                Assert.assertEquals(columns.getString("TABLE_SCHEM"), "test");
                Assert.assertEquals(columns.getString("TABLE_NAME"), "testMetadata");
                Assert.assertEquals(columns.getString("COLUMN_NAME"), "foo");
                Assert.assertEquals(columns.getInt("DATA_TYPE"), Types.FLOAT);
                Assert.assertEquals(columns.getString("TYPE_NAME"), "Float32");
                Assert.assertEquals(columns.getInt("COLUMN_SIZE"), 8);
                Assert.assertEquals(columns.getInt("BUFFER_LENGTH"), 0);
                Assert.assertEquals(columns.getInt("DECIMAL_DIGITS"), 8);
                Assert.assertEquals(columns.getInt("NUM_PREC_RADIX"), 10);
                Assert.assertEquals(columns.getInt("NULLABLE"), DatabaseMetaData.columnNoNulls);
                Assert.assertNull(columns.getObject("REMARKS"));
                Assert.assertNull(columns.getObject("COLUMN_DEF"));
                Assert.assertNull(columns.getObject("SQL_DATA_TYPE"));
                Assert.assertNull(columns.getObject("SQL_DATETIME_SUB"));
                Assert.assertEquals(columns.getInt("CHAR_OCTET_LENGTH"), 0);
                Assert.assertEquals(columns.getInt("ORDINAL_POSITION"), 1);
                Assert.assertEquals(columns.getString("IS_NULLABLE"), "NO");
                Assert.assertNull(columns.getObject("SCOPE_CATALOG"));
                Assert.assertNull(columns.getObject("SCOPE_SCHEMA"));
                Assert.assertNull(columns.getObject("SCOPE_TABLE"));
                Assert.assertNull(columns.getObject("SOURCE_DATA_TYPE"));
                Assert.assertEquals(columns.getString("IS_AUTOINCREMENT"), "NO");
                Assert.assertEquals(columns.getString("IS_GENERATEDCOLUMN"), "NO");
            }
        });
    }

    @Test
    public void testDatabaseVersion() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connection) throws Exception {
                String dbVersion = connection.getMetaData().getDatabaseProductVersion();
                Assert.assertFalse(dbVersion == null || dbVersion.isEmpty());
                int dbMajor = Integer.parseInt(dbVersion.substring(0, dbVersion.indexOf(".")));
                Assert.assertTrue(dbMajor > 0);
                Assert.assertEquals(connection.getMetaData().getDatabaseMajorVersion(), dbMajor);
                int majorIdx = dbVersion.indexOf(".") + 1;
                int dbMinor = Integer.parseInt(dbVersion.substring(majorIdx, dbVersion.indexOf(".", majorIdx)));
                Assert.assertEquals(connection.getMetaData().getDatabaseMinorVersion(), dbMinor);
            }
        });
    }

    @Test()
    public void testGetTablesEngines() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connection) throws Exception {
                String[] engines = new String[] {
                        "TinyLog",
                        "Log",
                        "Memory",
                        "MergeTree(foo, (foo), 8192)"
                };
                for (String engine : engines) {
                    connection.createStatement().executeQuery(
                            "DROP TABLE IF EXISTS test.testMetadata");
                    connection.createStatement().executeQuery(
                            "CREATE TABLE test.testMetadata("
                                    + "foo Date) ENGINE = "
                                    + engine);
                    ResultSet tableMeta = connection.getMetaData().getTables(null, "test", "testMetadata", null);
                    tableMeta.next();
                    Assert.assertEquals("TABLE", tableMeta.getString("TABLE_TYPE"));
                }
            }
        });
    }

    @Test
    public void testGetTablesViews() throws Exception {
        withNewConnection(new WithConnection() {
            @Override
            public void apply(Connection connection) throws Exception {
                connection.createStatement().executeQuery(
                        "DROP TABLE IF EXISTS test.testMetadataView");
                connection.createStatement().executeQuery(
                        "CREATE VIEW test.testMetadataView AS SELECT 1 FROM system.tables");
                ResultSet tableMeta = connection.getMetaData().getTables(
                        null, "test", "testMetadataView", null);
                tableMeta.next();
                Assert.assertEquals("VIEW", tableMeta.getString("TABLE_TYPE"));
            }
        });
    }


}

