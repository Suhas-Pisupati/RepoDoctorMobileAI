
import os
from dotenv import load_dotenv

load_dotenv()

GROQ_API_KEY = os.getenv("GROQ_API_KEY", "")
GROQ_MODEL = os.getenv("GROQ_MODEL", "")
GITHUB_TOKEN = os.getenv("GITHUB_TOKEN", "")

DATA_DIR = "data"
MAX_FILES = 100
MAX_FILE_BYTES = 150_000
MAX_TOTAL_CHARS = 1_000_000
