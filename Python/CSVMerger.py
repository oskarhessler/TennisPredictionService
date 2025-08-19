import pandas as pd
import os

# Mapp med original CSV-filer
folder_path = os.path.join("..","Data","OriginalCSVs")
# Mapp för output
folder_path2 = os.path.join("..","Data")

# Hämta alla CSV-filer och sortera
csv_files = sorted([f for f in os.listdir(folder_path) if f.endswith(".csv")])

# Filtrera filer mellan 2005 och 2024
csv_files_filtered = [f for f in csv_files if 2005 <= int(f.split(".")[0]) <= 2025]

df_list = []
for i, file in enumerate(csv_files_filtered):
    file_path = os.path.join(folder_path, file)
    try:
        df = pd.read_csv(file_path, encoding="utf-8")
    except UnicodeDecodeError:
        # Fallback om UTF-8 misslyckas
        df = pd.read_csv(file_path, encoding="latin1")
    df_list.append(df)

# Slå ihop alla DataFrames
merged_df = pd.concat(df_list, ignore_index=True)

# Spara output
output_file = os.path.join(folder_path2, "merged2005_2025.csv")
merged_df.to_csv(output_file, index=False, encoding="utf-8")

print(f"Merged {len(csv_files_filtered)} files into {output_file} with single header.")
