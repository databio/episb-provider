from datastructures import *
from postgres import conn
from fastapi import FastAPI, HTTPException, Query
import psycopg2
from starlette.responses import FileResponse
import tempfile
from datetime import datetime

# some global defaults
limit = 100
author = Author(**dict(family_name="Sheffield", given_name="Nathan", email="nsheff@virginia.edu"))
study = Study(**(dict(author=author, manuscript="", description="Default study", date=datetime.ctime(datetime.now()))))

app = FastAPI()

@app.get("/")
async def root():
    return {"message": "EPISB HUB by Databio lab"}

@app.get("/segments/get/fromSegment/:chr/:start/:end")
async def fromSegment(chr:str, start:int, end:int, all:bool=None):
    # validate start/end input
    if start>end:
        return {"message": "start value > end value"}
    if start<0 or end<0:
        return {"message": "start or end value < 0"}
    # validate chr input
    if not chr.startswith("chr"):
        chr = "chr"+chr.upper()
    elif (chr.startswith("CHR") or chr.startswith("chr")):
        chr = "chr" + chr[3:].upper()
    # hate leaving a dangling else
    # check to see if we are within possible chromosomes
    if not chr in chrom_enum:
        return {"message": "Error: chromosome entered is not correct"}
    # define sql query (hardcoded here for now)
    sqlq = """SELECT * FROM segments WHERE chrom = %s AND start > %s AND "end" < %s"""
    if all is None or (all is not None and not all):
        sqlq += " LIMIT(%d)" % limit
    # run postgres query at this point
    try:
        cur = conn.cursor()
        cur.execute(sqlq, (chr, start, end))
        dbres = cur.fetchall()
        res = [Region(**dict(id=dbr[0],seg_name=dbr[1],chr=dbr[2],start=dbr[3],end=dbr[4],)) for dbr in dbres]
        return {"message": res}
    except psycopg2.DatabaseError as pgerror:
        raise HTTPException(status_code=500, detail="Database error")
    except Exception as e:
        raise HTTPException(status_code=500, detail=e.args[0])
    finally:
        if cur is not None:
            cur.close()

# segID is seg_name::int
@app.get("/segments/find/BySegmentID/:segID")
async def findBySegmentID(segID:str=Query(default="", min_length=3, regex="^[a-zA-Z0-9]+::[0-9]+"), all:bool=True):
    seg_groups = segID.split("::")
    seg_name = seg_groups[0]
    segID = int(seg_groups[1])
    if segID < 0:
        return {"message": "segID must be a positive number."}
    sqlq = """SELECT * FROM segments WHERE segmentid = %s AND segmentation_name = %s"""
    if all is None or (all is not None and not all):
        sqlq += " LIMIT(%d)" % limit
    try:
        cur = conn.cursor()
        cur.execute(sqlq, [segID,seg_name])
        res = cur.fetchall()
        if (len(res)>0):
            dbr = res[0]
            return {"message": Region(**dict(id=dbr[0],seg_name=dbr[1],chr=dbr[2],start=dbr[3],end=dbr[4],))}
        else:
            return {"message": "not found"}
    except psycopg2.DatabaseError as pgerror:
        raise HTTPException(status_code=500, detail="Database error")
    except Exception as e:
        raise HTTPException(status_code=500, detail=e.args[0])
    finally:
        if cur is not None:
            cur.close()

@app.get("/segmentations/get/ByName/:segName")
async def getSegmentationByName(segName:str = Query(default="", min_length=1, regex="^[a-zA-Z0-9]+"), all:bool=None):
    class TempRes(BaseModel):
        segID: int

    sqlq = """SELECT segmentid FROM segments WHERE segmentation_name = %s"""
    if all is None or (all is not None and not all):
        sqlq += " LIMIT(%d)" % limit
    try:
        cur = conn.cursor()
        cur.execute(sqlq, [segName])
        dbres = cur.fetchall()
        res = [TempRes(**dict(segID=dbr[0])) for dbr in dbres]
        return {"message": res}
    except psycopg2.DatabaseError as pgerror:
        raise HTTPException(status_code=500, detail="Database error")
    except Exception as e:
        raise HTTPException(status_code=500, detail=e.args[0])
    finally:
        if cur is not None:
            cur.close()

@app.get("/segments/get/BySegmentationName/:segName")
async def getSegmentsBySegmentationName(segName:str = Query(default="", min_length=1, regex="^[a-zA-Z0-9]+"), all:bool=None):
    sqlq = """SELECT * FROM segments WHERE segmentation_name = %s"""
    if all is None or (all is not None and not all):
        sqlq += " LIMIT(%d)" % limit
    try:
        cur = conn.cursor()
        cur.execute(sqlq, [segName])
        dbres = cur.fetchall()
        res = [Region(**dict(id=dbr[0],seg_name=dbr[1],chr=dbr[2],start=dbr[3],end=dbr[4],)) for dbr in dbres]
        return {"message": res}
    except psycopg2.DatabaseError as pgerror:
        raise HTTPException(status_code=500, detail="Database error")
    except Exception as e:
        raise HTTPException(status_code=500, detail=e.args[0])
    finally:
        if cur is not None:
            cur.close()

@app.get("/segmentations/list/all")
async def listSegmentations():
    class TempRes(BaseModel):
        seg_name: str

    sqlq = """SELECT * FROM segmentations"""
    try:
        cur = conn.cursor()
        cur.execute(sqlq)
        dbres = cur.fetchall()
        res = [TempRes(**dict(seg_name=dbr[0])) for dbr in dbres]
        return {"message": res}
    except psycopg2.DatabaseError as pgerror:
        raise HTTPException(status_code=500, detail="Database error")
    except Exception as e:
        raise HTTPException(status_code=500, detail=e.args[0])
    finally:
        if cur is not None:
            cur.close()

# get all annotation values by experiment name
# optional parameters are operations >/</= and values
# FIXME: incomplete since it does not pull in experiment and study info from database, just makes it up!
@app.get("/experiments/get/ByName/:expName")
async def getAnnotationsByExperimentName(expName:str, op1:str=None, op2:str=None, val1:float=None, val2:float=None, all:bool=None):
    experiment = Experiment(**dict(name=expName, protocol="",cell_type="", species="", tissue="", antibody="", treatment="", description=""))

    # basic query below
    sqlq = """SELECT * FROM annotations WHERE exp_name = %s"""
    # add up the rest of the query if parameters were passed in
    if op1 is not None and val1 is not None:
        sqlq_ap1 = """ AND value %s %f """ % (op1, val1)
        sqlq += sqlq_ap1
    if op2 is not None and val2 is not None:
        sqlq_ap2 = """ AND value %s %f """ % (op2, val2)
        sqlq += sqlq_ap2
    if all is None or (all is not None and not all):
        sqlq += " LIMIT(%d)" % limit
    res = []
    try:
        # use a server side cursor to speed things up
        cur = conn.cursor('server_side_cursor')
        cur.execute(sqlq, [expName])
        dbres = cur.fetchall()
        res = [Annotation(**dict(regionID=ann[1]+"::"+str(ann[2]), value=ann[3], experiment=experiment, study=study)) for ann in dbres]
        return {"message": res}
    except psycopg2.DatabaseError as pgerror:
        raise HTTPException(status_code=500, detail="Database error")
    except Exception as e:
        raise HTTPException(status_code=500, detail=e.args[0])
    finally:
        if cur is not None:
            cur.close()

@app.get("/experiments/get/BySegmentationName/:segName")
async def getAnnotationsBySegmentationName(segName:str = Query(default="", min_length=1, regex="^[a-zA-Z0-9]+"), matrix:bool=None, all:bool=None):
    # basic query below
    sqlq = """SELECT * FROM annotations WHERE segmentation_name = %s"""
    # add up the rest of the query if parameters were passed in
    if matrix is not None:
        # here we serve the results as a .gz file
        sqlq_ap2 = """ GROUP BY exp_name"""
        sqlq += sqlq_ap2
    if all is None or (all is not None and not all):
        sqlq += " LIMIT(%d)" % limit
    try:
        # use a server side cursor to speed things up
        cur = conn.cursor('server_side_cursor')
        cur.execute(sqlq, [segName])
        dbres = cur.fetchall()
        print(dbres[0])
        res = []
        for ann in dbres:
            experiment = Experiment(**dict(name=ann[4], protocol="",cell_type="", species="", tissue="", antibody="", treatment="", description=""))
            res = [Annotation(**dict(regionID=ann[1]+"::"+str(ann[2]), value=ann[3], experiment=experiment, study=study)) for ann in dbres]
        return {"message": res}        
        # create the output file
        #with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as f_out:
        #    while True:
        #        # fetch in 1000 increments
        #        rows = cur.fetchmany(1000)
        #        if not rows:
        #            break
        #        for item in rows:
        #            f_out.write(','.join(map(str, item))+'\n')
        #    return FileResponse(f_out.name, media_type="text/plain")
    except psycopg2.DatabaseError as pgerror:
        raise HTTPException(status_code=500, detail="Database error")
    except Exception as e:
        raise HTTPException(status_code=500, detail=e.args[0])
    finally:
        if cur is not None:
            cur.close()
