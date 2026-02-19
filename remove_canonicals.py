#!/usr/bin/env python3
"""
Remove canonical URLs from all documentation files.
"""

import os
import frontmatter
from pathlib import Path

def remove_canonical(file_path: Path):
    """Remove canonical URL from a single file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if 'canonical:' not in content:
            return False
            
        post = frontmatter.loads(content)
        if 'canonical' in post.metadata:
            del post.metadata['canonical']
            
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(frontmatter.dumps(post))
            return True
            
    except Exception as e:
        print(f"❌ Error processing {file_path}: {str(e)}")
        return False

def process_directory(directory: Path):
    """Process all markdown files in directory."""
    files_processed = 0
    files_updated = 0
    
    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith(('.md', '.mdx')):
                file_path = Path(root) / file
                files_processed += 1
                
                if remove_canonical(file_path):
                    files_updated += 1
                    print(f"✅ Removed canonical from: {file_path.relative_to(directory)}")
    
    print("\n📊 Canonical URL Removal Complete")
    print("=" * 40)
    print(f"Files processed: {files_processed}")
    print(f"Files updated: {files_updated}")

if __name__ == "__main__":
    base_dir = Path("/Users/laptopwala1/bytechef-docs/docs/content/docs")
    print("🚀 Starting to remove canonical URLs...\n")
    process_directory(base_dir)
