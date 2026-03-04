#!/usr/bin/env python3
import requests
import time
import os

# Alternative API endpoint
API_URL = "https://api-inference.huggingface.co/models/runwayml/stable-diffusion-v1-5"
headers = {"Authorization": "Bearer hf_demo"}

def generate_image(prompt, filename):
    """Generate image using Hugging Face API"""
    try:
        response = requests.post(
            API_URL, 
            headers=headers, 
            json={"inputs": prompt},
            timeout=180
        )
        if response.status_code == 200:
            with open(filename, 'wb') as f:
                f.write(response.content)
            # Verify it's a valid image
            with open(filename, 'rb') as f:
                header = f.read(8)
                if header.startswith(b'\x89PNG'):
                    print(f"✓ Generated: {filename}")
                    return True
                else:
                    print(f"✗ Invalid image: {filename}")
                    return False
        else:
            print(f"✗ Failed {filename}: HTTP {response.status_code}")
            return False
    except Exception as e:
        print(f"✗ Error {filename}: {e}")
        return False

os.chdir('/root/.openclaw/workspace/小说/插图')

# Missing prompts
prompts = [
    ("Watercolor style illustration, warm healing tones. Young Chinese woman working late at night in small rental apartment, desk lamp casting warm glow, side silhouette profile, cozy but lonely atmosphere, soft lighting", "chapter-010.png"),
    
    ("Watercolor style illustration, emotional hospital scene. Young Chinese woman standing in hospital corridor, worried but strong determined expression, warm lighting from windows, family concern atmosphere", "chapter-020.png"),
    
    ("Watercolor illustration, office drama scene. Young Chinese woman in modern office, surprised shocked expression looking at new boss, professional setting, warm tones, business attire", "chapter-021.png"),
    
    ("Watercolor celebration scene, warm joyful tones. Chinese couple toasting with champagne glasses, celebrating project success, modern office or restaurant setting, happy expressions", "chapter-030.png"),
    
    ("Watercolor romantic scene, warm tones. Handsome mature Chinese man holding beautiful bouquet of flowers, sincere loving expression, romantic warm lighting, love confession moment, elegant", "chapter-031.png"),
]

success_count = 0
for prompt, filename in prompts:
    if os.path.exists(filename):
        # Check if valid
        with open(filename, 'rb') as f:
            if f.read(8).startswith(b'\x89PNG'):
                print(f"✓ Already exists: {filename}")
                success_count += 1
                continue
    
    print(f"Generating {filename}...")
    if generate_image(prompt, filename):
        success_count += 1
    time.sleep(3)

print(f"\nCompleted: {success_count}/{len(prompts)} images generated")
