import psycopg2

o = open("wgEncodeBroadHmmGm12878HMM_out.bed", "r")

conn = psycopg2.connect(host="localhost", database="episb", user="postgres", password="episb123")
cur = conn.cursor()

sql1 = """INSERT INTO segmentations(name) VALUES (%s)"""

# commit into segmentations table
cur.execute(sql1, ("BroadHMM",))

sql2 = """INSERT INTO segments(segmentation_name,chrom,start,\"end\") VALUES (%s,%s, %s, %s)"""

# load segments first
sqls = []
for line in o:
    s = line.split("\t")
    sqls.append(("BroadHMM", s[1], s[2], s[3]))
try:
    cur.executemany(sql2, sqls)
    conn.commit()
    cur.close()
except (Exception, psycopg2.DatabaseError) as error:
    print(error)
finally:
    conn.close()
