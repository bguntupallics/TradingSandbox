import { Github, Linkedin } from 'lucide-react'

export function Footer() {
    return (
        <footer className="site-footer">
            <span className="footer-credit">Built by Bhargav Guntupalli</span>
            <div className="footer-links">
                <a href="https://github.com/bguntupallics/TradingSandbox" target="_blank" rel="noopener noreferrer" aria-label="GitHub">
                    <Github size={20} />
                </a>
                <a href="https://www.linkedin.com/in/bguntupalli/" target="_blank" rel="noopener noreferrer" aria-label="LinkedIn">
                    <Linkedin size={20} />
                </a>
            </div>
        </footer>
    )
}
