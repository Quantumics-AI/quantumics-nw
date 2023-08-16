import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-data-quality-create',
  templateUrl: './data-quality-create.component.html',
  styleUrls: ['./data-quality-create.component.scss']
})
export class DataQualityCreateComponent implements OnInit {

  public fg: FormGroup;
  public sourceTargetType: string = 'target';

  constructor(private fb: FormBuilder) {

  }

  ngOnInit(): void {
    this.fg = this.fb.group({
      ruleName: new FormControl('', Validators.required),
      ruleDescription: new FormControl('', Validators.required),
      type: ['target'],
      sourceDataSource: new FormControl('aws'),
      sourceDataConnection: new FormControl('aws1'),
      sourceBucket: new FormControl('bucket1'),
      sourceFolder: new FormControl('folder1'),

      targetDataSource: new FormControl('aws', Validators.required),
      targetDataConnection: new FormControl('aws1', Validators.required),
      targetBucket: new FormControl('bucket1', Validators.required),
      targetFolder: new FormControl('folder1', Validators.required),
    });
  }

  public selectedType(type: string): void {
    if (type === 'sourceAndTarget') {
      this.fg.get('sourceDataSource').setValidators([Validators.required]);
      this.fg.get('sourceDataConnection').setValidators([Validators.required]);
      this.fg.get('sourceBucket').setValidators([Validators.required]);
      this.fg.get('sourceFolder').setValidators([Validators.required]);
    } else {
      this.fg.get('sourceDataSource')?.clearValidators();
      this.fg.get('sourceDataConnection')?.clearValidators();
      this.fg.get('sourceBucket')?.clearValidators();
      this.fg.get('sourceFolder')?.clearValidators();
    }

    this.fg.get('sourceDataSource')?.updateValueAndValidity();
    this.fg.get('sourceDataConnection')?.updateValueAndValidity();
    this.fg.get('sourceBucket')?.updateValueAndValidity();
    this.fg.get('sourceFolder')?.updateValueAndValidity();
  }

  public continue(): void {

  }

  public showData(): void {
    debugger;
    if (!this.fg.valid) {
      alert('asdadsd');
    }
  }
}
