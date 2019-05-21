import psycopg2

# shared connection object for now
conn = psycopg2.connect(host="localhost", database="episb", user="postgres", password="episb123")

