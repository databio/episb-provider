import psycopg2, datetime, sys
from psycopg2.extensions import AsIs, quote_ident

if len(sys.argv) != 5:
    print "Usage %s <experiment_file> <column> <segmentation_name> <skip>" % sys.argv[0]
    sys.exit(-1)

try:
    o = open("/home/maketo/dev/sheffield/segmentations/dhs112_v3.bed", "r")
    conn = psycopg2.connect(host="localhost", database="episb", user="postgres", password="episb123")
    cur = conn.cursor()

    if sys.argv[4] == "noskip":
        cur.execute("INSERT INTO Authors(fname, lname, email) VALUES (%s,%s,%s)",
                ("Nathan", "Sheffield", "nsheff@virginia.edu"))

        cur.execute("""INSERT INTO Studies(author_email, manuscript, description, sdate) VALUES(%s, NULL, NULL, %s)""",
                ("nsheff@virginia.edu", str(datetime.date.today())))

        conn.commit()

    sql_exp = """INSERT INTO Experiments(name, author_email, cell_type, species, tissue, antibody, treatment, description)
       VALUES (%s, %s, NULL, NULL, NULL, NULL, NULL, NULL)"""

    sql = """INSERT INTO Annotations(segmentation_name, segmentID, value, exp_name, study_id) 
            VALUES (%s, %s, %s, %s, %s)"""

    sqls = []
    cnt = 0
    ln = o.readline()
    col = int(sys.argv[2])
    segm_name = sys.argv[3]
    exp_names = ln.split(" ")
    if col > len(exp_names):
        print("Bad column number")
    else:
        exp_name = exp_names[col]
        cur.execute(sql_exp, (exp_name, "nsheff@virginia.edu"))
        conn.commit()

        for line in o:
            s = line.split(" ")
            sqls.append((segm_name, str(cnt), s[col], exp_name, 1))
            cnt += 1
        cur.executemany(sql, sqls)
        conn.commit()

except (Exception, psycopg2.DatabaseError) as error:
    print(error)
finally:
    conn.close()
    o.close()
