from psycopg2.pool import ThreadedConnectionPool
import sys

# shared connection object for now
threaded_conn_pool = ThreadedConnectionPool(5, 20, user="postgres", password="episb123", host="localhost", database="episb")
if not threaded_conn_pool:
    print("Could not obtain connection pool to Postgres database. Shutting down...")
    sys.exit(-1)
