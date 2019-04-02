import psycopg2
import sys

if len(sys.argv) != 3:
    print "Usage: %s <segments_file> <name_of_segmentation>" % sys.argv[0]
    sys.exit(-1)

try:
    o = open(sys.argv[1], "r")

    conn = psycopg2.connect(host="localhost", database="episb", user="postgres", password="episb123")
    cur = conn.cursor()

    sql1 = """INSERT INTO segmentations(name) VALUES (%s)"""

    # commit into segmentations table
    cur.execute(sql1, (sys.argv[2],))

    sql2 = """INSERT INTO segments(segmentID, segmentation_name,chrom,start,\"end\") VALUES (%s,%s,%s,%s,%s)"""

    # load segments first
    # read header line first and skip it
    o.readline()
    sql_lines = []
    cnt = 0
    for line in o:
        s = line.split(" ")
        sql_lines.append((str(cnt),sys.argv[2],s[0],s[1],s[2]))
        cnt = cnt + 1
    
    cur.executemany(sql2, sql_lines)
    conn.commit()
    cur.close()
except (Exception, psycopg2.DatabaseError) as error:
    print(error)
finally:
    conn.close()
    o.close()
