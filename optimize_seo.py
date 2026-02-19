#!/usr/bin/env python3
"""
ByteChef Docs - SEO Optimizer
Scans and optimizes all documentation pages for better SEO.
"""

import re
import os
from pathlib import Path
from typing import Dict, List, Set, Optional
import frontmatter
import hashlib
from collections import defaultdict

class SEOOptimizer:
    def __init__(self, base_dir: str):
        self.base_dir = Path(base_dir)
        self.files_processed = 0
        self.files_updated = 0
        self.duplicate_titles = defaultdict(list)
        self.descriptions = {}
        self.title_to_path = {}

    def generate_seo_title(self, file_path: Path) -> str:
        """Generate an SEO-optimized title from file path."""
        rel_path = str(file_path.relative_to(self.base_dir))
        title = rel_path.replace('.mdx', '').replace('.md', '')
        title = ' '.join(word.capitalize() 
                       for part in title.split('/') 
                       for word in part.split('-'))
        
        # Special handling for versioned components
        if ' V1' in title:
            title = title.replace(' V1', '')
        
        return title

    def generate_seo_description(self, title: str, content: str) -> str:
        """Generate an SEO-optimized description."""
        # Extract first meaningful paragraph
        first_paragraph = ''
        for line in content.split('\n'):
            line = line.strip()
            if line and not line.startswith(('#', 'import', 'export', '---', '```')):
                first_paragraph = line
                break
        
        # Build description
        if first_paragraph and len(first_paragraph) > 30:
            description = first_paragraph
        else:
            description = f"Learn how to use {title} in ByteChef."
        
        # Ensure length is between 120-160 characters
        if len(description) < 120:
            description += f" Comprehensive guide with examples and best practices for {title}."
            description = description[:157] + '...' if len(description) > 160 else description
        elif len(description) > 160:
            description = description[:157] + '...'
        
        # Ensure uniqueness
        desc_hash = hashlib.md5(description.encode()).hexdigest()
        if desc_hash in self.descriptions:
            parent_dir = title.split()[-1] if ' ' in title else ''
            description = f"{description} {parent_dir} in ByteChef platform.".strip()
            description = description[:157] + '...' if len(description) > 160 else description
        
        self.descriptions[hashlib.md5(description.encode()).hexdigest()] = description
        return description

    def process_file(self, file_path: Path) -> bool:
        """Process a single file for SEO optimization."""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            if not content.startswith('---'):
                print(f"⚠️  No frontmatter in {file_path.relative_to(self.base_dir)}")
                return False

            post = frontmatter.loads(content)
            updated = False

            # Handle title
            current_title = post.metadata.get('title', '')
            new_title = self.generate_seo_title(file_path)
            
            if not current_title or current_title in self.duplicate_titles:
                if current_title:
                    self.duplicate_titles[current_title].append(str(file_path))
                post.metadata['title'] = new_title
                updated = True
            else:
                self.duplicate_titles[current_title] = [str(file_path)]
                new_title = current_title

            # Handle description
            current_desc = post.metadata.get('description', '')
            new_desc = self.generate_seo_description(new_title, content)
            
            if not current_desc or current_desc == new_desc or len(current_desc) < 100:
                post.metadata['description'] = new_desc
                updated = True

            # Add/update canonical URL
            rel_path = str(file_path.relative_to(self.base_dir))
            canonical = f"https://bytechef.io/docs/{rel_path.replace('.mdx', '').replace('.md', '')}"
            if 'canonical' not in post.metadata:
                post.metadata['canonical'] = canonical
                updated = True

            # Save if updated
            if updated:
                new_content = frontmatter.dumps(post)
                with open(file_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                self.files_updated += 1
                return True

            return False

        except Exception as e:
            print(f"❌ Error processing {file_path.relative_to(self.base_dir)}: {str(e)}")
            return False

    def process_directory(self, directory: Optional[Path] = None):
        """Process all markdown files in directory."""
        if directory is None:
            directory = self.base_dir

        for item in directory.iterdir():
            if item.is_dir():
                self.process_directory(item)
            elif item.suffix in ['.md', '.mdx']:
                self.files_processed += 1
                self.process_file(item)

    def generate_report(self) -> str:
        """Generate a report of changes made."""
        report = [
            "📊 SEO Optimization Report",
            "=" * 80,
            f"Files processed: {self.files_processed}",
            f"Files updated: {self.files_updated}",
            f"Duplicate titles found: {len([t for t, v in self.duplicate_titles.items() if len(v) > 1])}",
            "\n🔍 Duplicate Titles:",
            "-" * 40
        ]

        for title, files in self.duplicate_titles.items():
            if len(files) > 1:
                report.append(f"\n'{title}' found in:")
                for f in files:
                    report.append(f"  - {f}")

        report.extend([
            "\n" + "=" * 80,
            "✅ Optimization complete!",
            "\nNext steps:",
            "1. Review changes: git diff",
            "2. Commit changes: git add . && git commit -m 'docs: optimize SEO metadata'",
            "3. Push changes: git push"
        ])

        return "\n".join(report)

def main():
    base_dir = Path("/Users/laptopwala1/bytechef-docs/docs/content/docs")
    optimizer = SEOOptimizer(base_dir)
    
    print("🚀 Starting SEO optimization...")
    optimizer.process_directory()
    
    print("\n" + optimizer.generate_report())
    
    with open("seo_optimization_report.txt", "w", encoding="utf-8") as f:
        f.write(optimizer.generate_report())
    
    print("\n📝 Full report saved to: seo_optimization_report.txt")

if __name__ == "__main__":
    main()
