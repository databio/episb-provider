from fastapi import FastAPI
from pydantic import BaseModel

# Use for returning text See also FileResponse
from starlette.responses import Response


# Takes an async generator and streams the response body.
from starlette.responses import StreamingResponse
import asyncio

app = FastAPI()

# from starlette.middleware.gzip import GZipMiddleware
# app.add_middleware(GZipMiddleware, minimum_size=1000000)

# These are Pydantic classes

class Item(BaseModel):
    name: str
    price: float
    is_offer: bool = None




class Region(BaseModel):
    id: str
    start: int
    end: int
    def __str__(self):
        return "{}\n".format("\t".join(map(str, [self.id, self.start, self.end])))


class RegionSet(BaseModel):
    id: str
    regions: list
    def __str__(self):
        return "".join(map(str, self.regions))


class Segmentation(BaseModel):
    id: str
    segments: list


class Segment(Region):
    segmentation: Segmentation



async def slow_numbers(minimum, maximum):
    yield('<html><body><ul>')
    for number in range(minimum, maximum + 1):
        yield '<li>%d</li>' % number
        await asyncio.sleep(.5)
    yield('</ul></body></html>')


ids = range(1, 150000)
starts = range(1, 150000)
ends = range(1, 150000)


rs = []
try:
    for i in ids:
        rs.append(Region(id=ids[i], start=starts[i], end=ends[i]))
except:
    pass

a=RegionSet(id=1, regions=rs)


database = {
    "1": a,
    "2": None
}


print("Ready...")

@app.get("/")
def read_root():
    return {"Hello": "World"}


@app.get("/segments/{item_id}")
def read_item(item_id: int, q: str = None, blah: str = None):
    return {"item_id": item_id, "q": q, "blah": blah}



@app.get("/regionset/{item_id}")
def get_region_set(item_id: int):
    return Response(str(database[str(item_id)]), media_type='text/plain')



@app.put("/items/{item_id}")
def create_item(item_id: int, item: Item):
    return {"item_name": item.name, "item_id": item_id}


@app.get("/stream")
async def get_streaming_response():
    generator = slow_numbers(1, 100)
    return StreamingResponse(generator, media_type='text/html')


@app.get("/nostream")
def get_streaming_response():
    generator = slow_numbers(1, 100)
    return Response(generator)
