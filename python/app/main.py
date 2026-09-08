"""Application assembly, and the one place where errors become status codes.

Run it with:  uvicorn app.main:app --port 8080
or with:      scripts/run.sh python   (scripts\\run.ps1 python on Windows)
"""

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app import routers, services

app = FastAPI(
    title="Core Accounts API",
    version="1.0.0",
    description="System API over the credit union's core accounts ledger.",
)
app.include_router(routers.router)


@app.exception_handler(services.BadRequestError)
async def handle_bad_request(request: Request, error: services.BadRequestError):
    return JSONResponse(
        status_code=400, content={"error": "BAD_REQUEST", "message": str(error)}
    )


@app.exception_handler(services.NotFoundError)
async def handle_not_found(request: Request, error: services.NotFoundError):
    return JSONResponse(
        status_code=404, content={"error": "NOT_FOUND", "message": str(error)}
    )


@app.exception_handler(services.ValidationError)
async def handle_validation_error(request: Request, error: services.ValidationError):
    return JSONResponse(
        status_code=422,
        content={
            "error": "UNPROCESSABLE_ENTITY",
            "message": str(error),
            "fieldErrors": error.field_errors,
        },
    )
