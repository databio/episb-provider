from datastructures import *
from postgres import conn
from fastapi import FastAPI
import psycopg2

app = FastAPI()

@app.get("/")
async def root():
    return {"message": "EPISB HUB by Databio lab"}

@app.get("/segments/get/fromSegment/:chr/:start/:end")
async def fromSegment(chr:str, start:int, end:int):
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
    # run postgres query at this point
    try:
        cur = conn.cursor()
        cur.execute(sqlq, (chr, start, end))
        res = cur.fetchall()
        return {"message": res}
    except psycopg2.DatabaseError as pgerror:
        return {"error": pgerror.pgerror}
    except Exception as e:
        return {"error": e.args[0]}
    finally:
        if cur is not None:
            cur.close()
    
@app.get("/segments/find/BySegmentID/:segID")
async def findBySegmentID(segID:int):
    if segID < 0:
        return {"message": "segID must be a positive number."}
    sqlq = """SELECT * FROM segments WHERE segmentid = %s"""
    try:
        cur = conn.cursor()
        cur.execute(sqlq, [segID])
        res = cur.fetchall()
        return {"message": res}
    except psycopg2.DatabaseError as pgerror:
        return {"error": pgerror.pgerror}
    except Exception as e:
        return {"error": e.args[0]}
    finally:
        if cur is not None:
            cur.close()

@app.get("/segmentations/get/ByName/:segName")
async def getSegmentationByName(segName:str):
    sqlq = """SELECT segmentid FROM segments WHERE segmentation_name = %s"""
    try:
        cur = conn.cursor()
        cur.execute(sqlq, [segName])
        res = cur.fetchall()
        return {"message": res}
    except psycopg2.Error as error:
        return {"error": error.pgerror}
    except Exception as e:
        return {"error": e.args[0]}
    finally:
        if cur is not None:
            cur.close()

@app.get("/segments/get/BySegmentationName/:segName")
async def getSegmentsBySegmentationName(segName:str):
    sqlq = """SELECT * FROM segments WHERE segmentation_name = %s"""
    try:
        cur = conn.cursor()
        cur.execute(sqlq, [segName])
        res = cur.fetchall()
        return {"message": res}
    except psycopg2.Error as error:
        return {"error": error.pgerror}
    except Exception as e:
        return {"error": e.args[0]}
    finally:
        if cur is not None:
            cur.close()

@app.get("/segmentations/list/all")
async def listSegmentations():
    sqlq = """SELECT * FROM segmentations"""
    try:
        cur = conn.cursor()
        cur.execute(sqlq)
        res = cur.fetchall()
        return {"message": res}
    except psycopg2.Error as error:
        return {"error": error.pgerror}
    except Exception as e:
        return {"error": e.args[0]}
    finally:
        if cur is not None:
            cur.close()
