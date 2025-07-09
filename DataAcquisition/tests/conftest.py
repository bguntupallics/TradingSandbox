import sys
from pathlib import Path

# Path(__file__).parents[1] == the directory one level up from tests/ (i.e. project root)
project_root = Path(__file__).parents[1].resolve()
sys.path.insert(0, str(project_root))
