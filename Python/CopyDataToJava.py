import shutil
import os

project_root = r"C:\Users\Admin\Downloads\TennisPredictionService\TennisPredictionService"
source_file = os.path.join(project_root, "Data", "merged2005_2024.csv")  # <-- ändrat till _
resources_dir = os.path.join(project_root, "Java", "src", "main", "resources")
destination_file = os.path.join(resources_dir, "merged2005_2024.csv")

# Kontrollera om filen finns
if not os.path.exists(source_file):
    print(f"Källfilen hittades inte: {source_file}")
    exit(1)

# Skapa resources-mappen om den inte finns
os.makedirs(resources_dir, exist_ok=True)

# Kopiera filen
shutil.copy2(source_file, destination_file)
print(f"Filen kopierad till: {destination_file}")
