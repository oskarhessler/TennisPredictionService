import os
import requests

# Base URL for raw CSVs
base_url = "https://raw.githubusercontent.com/Tennismylife/TML-Database/master"

# Local save folder
output_dir = os.path.join("..","Data","OriginalCSVs")  # relative path from script to project root/Data
os.makedirs(output_dir, exist_ok=True)

# Files you want (could be automated by scraping GitHub API)
files = [
    #"ATP_Database.csv",
    #"ongoing_tourneys.csv",
    *[f"{year}.csv" for year in range(1968, 2025+1)]
    #1968-2025
]

for file in files:
    url = f"{base_url}/{file}"
    r = requests.get(url)
    if r.status_code == 200:
        with open(os.path.join(output_dir, file), "wb") as f:
            f.write(r.content)
        print(f"Downloaded {file}")
    else:
        print(f"Failed to download {file}")
